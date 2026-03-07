package banawa.isi.java_messagerie.server;

import banawa.isi.java_messagerie.dao.MessageDAO;
import banawa.isi.java_messagerie.dao.UserDAO;
import banawa.isi.java_messagerie.model.Message;
import banawa.isi.java_messagerie.model.User;
import banawa.isi.java_messagerie.network.NetworkMessage;
import at.favre.lib.crypto.bcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.stream.Collectors;

public class ClientHandler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    private final Socket socket;
    private final Server server;
    private ObjectInputStream  in;
    private ObjectOutputStream out;

    private String username;

    private final UserDAO    userDAO    = new UserDAO();
    private final MessageDAO messageDAO = new MessageDAO();

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in  = new ObjectInputStream(socket.getInputStream());

            while (true) {
                NetworkMessage msg = (NetworkMessage) in.readObject();
                if (msg == null) continue;

                switch (msg.getType()) {
                    case LOGIN            -> handleLogin(msg);
                    case REGISTER         -> handleRegister(msg);
                    case SEND_MESSAGE     -> handleMessage(msg);
                    case GET_ONLINE_USERS -> handleGetOnlineUsers();
                    case GET_HISTORY      -> handleGetHistory(msg);
                    case LOGOUT           -> { handleLogout(); return; }
                    default -> logger.warn("Unknown type: {}", msg.getType());
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            logger.warn("Connection lost for {}: {}", username, e.getMessage());
        } finally {
            cleanup();
        }
    }

    // RG1 username unique, RG9 mot de passe hache
    private void handleRegister(NetworkMessage msg) {
        String user = msg.getSender();
        String pass = msg.getContent();

        if (userDAO.usernameExists(user)) {
            safeSend(NetworkMessage.fail(NetworkMessage.Type.REGISTER_FAIL,
                    "Username already taken."));
            return;
        }

        String hashed = BCrypt.withDefaults().hashToString(12, pass.toCharArray());
        userDAO.save(new User(user, hashed));
        logger.info("[REGISTER] {}", user);
        safeSend(new NetworkMessage(NetworkMessage.Type.REGISTER_SUCCESS,
                "Account created. You can now log in."));
    }

    // RG3 : une seule session, RG4 : statut ONLINE
    private void handleLogin(NetworkMessage msg) {
        String user = msg.getSender();
        String pass = msg.getContent();

        User u = userDAO.findByUsername(user);

        if (u == null) {
            safeSend(NetworkMessage.fail(NetworkMessage.Type.LOGIN_FAIL,
                    "Invalid username or password."));
            return;
        }

        BCrypt.Result result = BCrypt.verifyer().verify(pass.toCharArray(), u.getPassword());
        if (!result.verified) {
            safeSend(NetworkMessage.fail(NetworkMessage.Type.LOGIN_FAIL,
                    "Invalid username or password."));
            return;
        }

        // RG3 : deja connecte ?
        if (server.isUserOnline(user)) {
            safeSend(NetworkMessage.fail(NetworkMessage.Type.LOGIN_FAIL,
                    "User already connected from another session."));
            return;
        }

        this.username = user;
        if (!server.addOnlineClient(username, this)) {
            safeSend(NetworkMessage.fail(NetworkMessage.Type.LOGIN_FAIL,
                    "Session conflict. Try again."));
            return;
        }

        // RG4 : statut ONLINE en DB
        userDAO.updateStatus(username, User.Status.ONLINE);

        safeSend(NetworkMessage.loginSuccess(username));
        logger.info("[LOGIN] {}", username);

        // ✅ CORRECTION : envoyer TOUS les utilisateurs avec leur statut
        handleGetOnlineUsers();

        // RG6 : livraison messages hors-ligne
        List<Message> pending = messageDAO.getUndeliveredMessages(username);
        for (Message m : pending) {
            NetworkMessage nm = new NetworkMessage(
                    NetworkMessage.Type.RECEIVE_MESSAGE,
                    m.getSender().getUsername(),
                    username,
                    m.getContenu());
            nm.setTimestamp(m.getDateEnvoi());
            safeSend(nm);
            messageDAO.markAsDelivered(m);
        }
        if (!pending.isEmpty())
            logger.info("[OFFLINE_DELIVERY] {} messages to {}", pending.size(), username);
    }

    // RG2, RG5, RG6, RG7
    private void handleMessage(NetworkMessage msg) {
        if (username == null) return;

        String content = msg.getContent();

        // RG7
        if (content == null || content.isBlank() || content.length() > 1000) {
            safeSend(NetworkMessage.fail(NetworkMessage.Type.ERROR,
                    "Message must be 1-1000 characters."));
            return;
        }

        User sender   = userDAO.findByUsername(username);
        User receiver = userDAO.findByUsername(msg.getReceiver());

        // RG5
        if (receiver == null) {
            safeSend(NetworkMessage.fail(NetworkMessage.Type.ERROR,
                    "Recipient not found."));
            return;
        }

        Message entity = new Message(sender, receiver, content);
        messageDAO.save(entity);
        logger.info("[MSG] {} -> {}: {}", username, receiver.getUsername(), content);

        // RG6 : livraison directe si destinataire en ligne
        if (server.isUserOnline(receiver.getUsername())) {
            ClientHandler destHandler = server.getClientHandler(receiver.getUsername());
            NetworkMessage delivery = new NetworkMessage(
                    NetworkMessage.Type.RECEIVE_MESSAGE,
                    username,
                    receiver.getUsername(),
                    content);
            delivery.setTimestamp(entity.getDateEnvoi());
            destHandler.safeSend(delivery);
            messageDAO.markAsDelivered(entity);
        }
        // Sinon : reste ENVOYE en DB, livre a la prochaine connexion (RG6)
    }

    // ✅ CORRECTION PRINCIPALE : envoyer TOUS les utilisateurs (online + offline)
    //    sauf soi-même, avec leur statut réel
    private void handleGetOnlineUsers() {
        List<User> allUsers = userDAO.findAll();

        List<NetworkMessage.UserStatusDTO> dtos = allUsers.stream()
                .filter(u -> !u.getUsername().equals(username)) // exclure soi-même
                .map(u -> new NetworkMessage.UserStatusDTO(
                        u.getUsername(),
                        server.isUserOnline(u.getUsername()) // statut en temps réel
                ))
                .collect(Collectors.toList());

        safeSend(new NetworkMessage(NetworkMessage.Type.ONLINE_USERS_RESPONSE, dtos));
    }

    // RG8 : historique ordre chronologique
    private void handleGetHistory(NetworkMessage msg) {
        if (username == null) return;
        String partner = msg.getReceiver();
        List<Message> history = messageDAO.getHistory(username, partner);

        List<NetworkMessage> historyMsgs = history.stream().map(m -> {
            NetworkMessage nm = new NetworkMessage(
                    NetworkMessage.Type.RECEIVE_MESSAGE,
                    m.getSender().getUsername(),
                    m.getReceiver().getUsername(),
                    m.getContenu());
            nm.setTimestamp(m.getDateEnvoi());
            return nm;
        }).collect(Collectors.toList());

        safeSend(new NetworkMessage(NetworkMessage.Type.HISTORY_RESPONSE, historyMsgs));
    }

    // RG4 : statut OFFLINE a la deconnexion
    private void handleLogout() {
        cleanup();
        logger.info("[LOGOUT] {}", username);
    }

    public synchronized void safeSend(NetworkMessage msg) {
        try {
            out.writeObject(msg);
            out.flush();
            out.reset();
        } catch (IOException e) {
            logger.warn("Send failed to {}: {}", username, e.getMessage());
        }
    }

    public void sendStatusUpdate(String changedUser, boolean online) {
        safeSend(NetworkMessage.statusChange(changedUser, online));
    }

    private void cleanup() {
        if (username != null) {
            server.removeOnlineClient(username);
            userDAO.updateStatus(username, User.Status.OFFLINE);
            username = null;
        }
        try {
            if (in     != null) in.close();
            if (out    != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }
}
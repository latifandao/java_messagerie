package banawa.isi.java_messagerie.server;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.isil3gl.messageriejava.dao.MessageDAO;
import com.isil3gl.messageriejava.dao.UserDAO;
import com.isil3gl.messageriejava.model.Message;
import com.isil3gl.messageriejava.model.User;
import com.isil3gl.messageriejava.network.NetworkMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Optional;

public class ClientHandler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    private final Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String loggedInUsername = null;

    private final UserDAO userDAO = new UserDAO();
    private final MessageDAO messageDAO = new MessageDAO();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // Output stream must be created BEFORE input stream — always
            out = new ObjectOutputStream(socket.getOutputStream());
            in  = new ObjectInputStream(socket.getInputStream());

            NetworkMessage message;
            // Keep reading messages from this client until they disconnect
            while ((message = (NetworkMessage) in.readObject()) != null) {
                route(message);
            }

        } catch (IOException e) {
            // EOFException is a subclass of IOException so it's caught here too
            // EOFException specifically means the client cleanly disconnected
            if (e instanceof EOFException) {
                logger.info("Client disconnected cleanly: {}",
                        loggedInUsername != null ? loggedInUsername : "unauthenticated");
            } else {
                // RG10 — any other IO error means connection was lost
                logger.warn("Connection lost for {}: {}",
                        loggedInUsername != null ? loggedInUsername : "unauthenticated",
                        e.getMessage());
            }
            handleDisconnect();
        } catch (ClassNotFoundException e) {
            logger.error("Unknown message type received: {}", e.getMessage());
        } finally {
            handleDisconnect();
            closeConnection();
        }
    }

    // --- Router ---

    private void route(NetworkMessage message) {
        logger.info("Received {} from {}", message.getType(),
                loggedInUsername != null ? loggedInUsername : "unauthenticated");
        switch (message.getType()) {
            case REGISTER        -> handleRegister(message);
            case LOGIN           -> handleLogin(message);
            case LOGOUT          -> handleLogout();
            case SEND_MESSAGE    -> handleSendMessage(message);
            case GET_HISTORY     -> handleGetHistory(message);
            case GET_ONLINE_USERS -> handleGetOnlineUsers();
            default -> sendMessage(new NetworkMessage(
                    NetworkMessage.Type.ERROR, "Unknown message type"));
        }
    }

    // --- Handlers ---

    private void handleRegister(NetworkMessage msg) {
        String username = msg.getSender();
        String password = msg.getContent();

        // RG1 — username must be unique
        if (userDAO.existsByUsername(username)) {
            sendMessage(new NetworkMessage(
                    NetworkMessage.Type.REGISTER_FAIL, "Username already taken"));
            return;
        }

        // RG9 — hash password before saving
        String hashed = BCrypt.withDefaults().hashToString(12, password.toCharArray());

        User user = new User(username, hashed);
        userDAO.save(user);

        logger.info("New user registered: {}", username);
        sendMessage(new NetworkMessage(NetworkMessage.Type.REGISTER_SUCCESS));
    }

    private void handleLogin(NetworkMessage msg) {
        String username = msg.getSender();
        String password = msg.getContent();

        // RG3 — only one session per user
        if (SessionManager.isAlreadyConnected(username)) {
            sendMessage(new NetworkMessage(
                    NetworkMessage.Type.LOGIN_FAIL, "User already connected"));
            return;
        }

        Optional<User> userOpt = userDAO.findByUsername(username);

        // Check user exists
        if (userOpt.isEmpty()) {
            sendMessage(new NetworkMessage(
                    NetworkMessage.Type.LOGIN_FAIL, "User not found"));
            return;
        }

        // RG9 — verify hashed password
        BCrypt.Result result = BCrypt.verifyer()
                .verify(password.toCharArray(), userOpt.get().getPassword());

        if (!result.verified) {
            sendMessage(new NetworkMessage(
                    NetworkMessage.Type.LOGIN_FAIL, "Incorrect password"));
            return;
        }

        // All good — register session
        loggedInUsername = username;
        SessionManager.addSession(username, this);

        // RG4 — set status to ONLINE
        userDAO.updateStatus(username, User.UserStatus.ONLINE);

        logger.info("User logged in: {}", username);
        sendMessage(new NetworkMessage(NetworkMessage.Type.LOGIN_SUCCESS, username));

        // Notify all other clients this user came online
        SessionManager.broadcastExcept(username,
                new NetworkMessage(NetworkMessage.Type.USER_CONNECTED, username));

        // RG6 — deliver any messages sent while offline
        deliverPendingMessages(username);
    }

    private void handleLogout() {
        logger.info("User logged out: {}", loggedInUsername);
        handleDisconnect();
    }

    private void handleSendMessage(NetworkMessage msg) {
        // RG2 — must be authenticated
        if (loggedInUsername == null) {
            sendMessage(new NetworkMessage(
                    NetworkMessage.Type.ERROR, "Not authenticated"));
            return;
        }

        String receiverUsername = msg.getReceiver();
        String content = msg.getContent();

        // RG7 — content must not be empty
        if (content == null || content.trim().isEmpty()) {
            sendMessage(new NetworkMessage(
                    NetworkMessage.Type.ERROR, "Message cannot be empty"));
            return;
        }

        // RG7 — content must not exceed 1000 characters
        if (content.length() > 1000) {
            sendMessage(new NetworkMessage(
                    NetworkMessage.Type.ERROR, "Message exceeds 1000 characters"));
            return;
        }

        // RG5 — receiver must exist
        Optional<User> receiverOpt = userDAO.findByUsername(receiverUsername);
        if (receiverOpt.isEmpty()) {
            sendMessage(new NetworkMessage(
                    NetworkMessage.Type.ERROR, "Recipient does not exist"));
            return;
        }

        // Save message to DB
        Optional<User> senderOpt = userDAO.findByUsername(loggedInUsername);
        Message message = new Message(senderOpt.get(), receiverOpt.get(), content);
        messageDAO.save(message);

        logger.info("Message from {} to {}", loggedInUsername, receiverUsername);

        // RG6 — if receiver is online deliver immediately, otherwise it stays in DB
        if (SessionManager.isOnline(receiverUsername)) {
            ClientHandler receiverHandler = SessionManager.getHandler(receiverUsername);
            receiverHandler.sendMessage(new NetworkMessage(
                    NetworkMessage.Type.RECEIVE_MESSAGE,
                    loggedInUsername,
                    receiverUsername,
                    content));

            // Mark as received
            messageDAO.updateStatus(message.getId(), Message.MessageStatus.RECU);
        }
    }

    private void handleGetHistory(NetworkMessage msg) {
        // RG2 — must be authenticated
        if (loggedInUsername == null) {
            sendMessage(new NetworkMessage(
                    NetworkMessage.Type.ERROR, "Not authenticated"));
            return;
        }

        // RG8 — history ordered chronologically (handled in DAO)
        List<Message> history = messageDAO.getConversation(
                loggedInUsername, msg.getReceiver());

        sendMessage(new NetworkMessage(NetworkMessage.Type.HISTORY_RESPONSE, history));
    }

    private void handleGetOnlineUsers() {
        List<String> onlineUsers = SessionManager.getOnlineUsernames();
        sendMessage(new NetworkMessage(
                NetworkMessage.Type.ONLINE_USERS_RESPONSE, onlineUsers));
    }

    // --- Offline message delivery (RG6) ---

    private void deliverPendingMessages(String username) {
        List<Message> pending = messageDAO.getPendingMessages(username);
        if (pending.isEmpty()) return;

        logger.info("Delivering {} pending messages to {}", pending.size(), username);

        for (Message msg : pending) {
            sendMessage(new NetworkMessage(
                    NetworkMessage.Type.RECEIVE_MESSAGE,
                    msg.getSender().getUsername(),
                    username,
                    msg.getContenu()));

            // Mark as received now that it's been delivered
            messageDAO.updateStatus(msg.getId(), Message.MessageStatus.RECU);
        }
    }

    // --- Disconnect handling ---

    private void handleDisconnect() {
        if (loggedInUsername != null) {
            // RG4 — set status to OFFLINE
            userDAO.updateStatus(loggedInUsername, User.UserStatus.OFFLINE);
            SessionManager.removeSession(loggedInUsername);

            // Notify all other clients
            SessionManager.broadcastExcept(loggedInUsername,
                    new NetworkMessage(
                            NetworkMessage.Type.USER_DISCONNECTED, loggedInUsername));

            logger.info("User disconnected: {}", loggedInUsername);
            loggedInUsername = null;
        }
    }

    private void closeConnection() {
        try {
            if (in != null)     in.close();
            if (out != null)    out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            logger.error("Error closing connection: {}", e.getMessage());
        }
    }

    // --- Send a message to THIS client ---

    public synchronized void sendMessage(NetworkMessage message) {
        try {
            out.writeObject(message);
            out.flush();
            // Reset prevents Hibernate caching old object state in the stream
            out.reset();
        } catch (IOException e) {
            logger.error("Error sending message to {}: {}", loggedInUsername, e.getMessage());
        }
    }
}

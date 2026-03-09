package banawa.isi.java_messagerie.server;

import banawa.isi.java_messagerie.network.NetworkMessage;
import banawa.isi.java_messagerie.utils.HibernateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    public  static final int    PORT   = 5000;

    private final Map<String, ClientHandler> clientsOnline = new ConcurrentHashMap<>();

    public void start() {
        HibernateUtil.getEntityManager().close();
        logger.info("Database initialised");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("Server listening on port {}", PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("New TCP connection from {}", clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket, this);
                Thread t = new Thread(handler);
                t.setDaemon(true);
                t.setName("Client-" + clientSocket.getInetAddress());
                t.start();
            }

        } catch (IOException e) {
            logger.error("Server fatal error: {}", e.getMessage());
        }
    }

    // RG3 : une seule session par utilisateur
    public synchronized boolean addOnlineClient(String username, ClientHandler handler) {
        if (clientsOnline.containsKey(username)) {
            logger.warn("Duplicate login attempt for {}", username);
            return false;
        }
        clientsOnline.put(username, handler);
        logger.info("[ONLINE]  {}", username);
        broadcastStatusChange(username, true);
        return true;
    }

    // RG4 : statut OFFLINE, previent les autres
    public void removeOnlineClient(String username) {
        if (clientsOnline.remove(username) != null) {
            logger.info("[OFFLINE] {}", username);
            broadcastStatusChange(username, false);
        }
    }

    public boolean       isUserOnline(String username)    { return clientsOnline.containsKey(username); }
    public ClientHandler getClientHandler(String username) { return clientsOnline.get(username); }

    public List<String> getOnlineUsernames() {
        return new ArrayList<>(clientsOnline.keySet());
    }

    // RG12 : broadcast changement de statut a tous les clients connectes
    private void broadcastStatusChange(String username, boolean online) {
        NetworkMessage msg = NetworkMessage.statusChange(username, online);
        clientsOnline.forEach((user, handler) -> {
            if (!user.equals(username)) {
                handler.safeSend(msg);   // ← safeSend (pas send) — pas de IOException
            }
        });
    }

    public static void main(String[] args) {
        new Server().start();
    }
}
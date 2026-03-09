package banawa.isi.java_messagerie.client;

import banawa.isi.java_messagerie.network.NetworkMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Connexion Singleton au serveur.
 * Tous les controllers utilisent getInstance() — ne jamais créer manuellement.
 */
public class ServerConnection {

    private static final Logger logger = LoggerFactory.getLogger(ServerConnection.class);

    private static final String HOST = "localhost";
    private static final int    PORT = 5000;

    private Socket             socket;
    private ObjectOutputStream out;
    private ObjectInputStream  in;
    private boolean            running = false;

    private Consumer<NetworkMessage> onMessageReceived;
    private Runnable                 onConnectionLost;

    // ── Singleton ─────────────────────────────────────────────────

    private static ServerConnection instance;

    public static ServerConnection getInstance() {
        if (instance == null) instance = new ServerConnection();
        return instance;
    }

    private ServerConnection() {}

    // ── Connexion ─────────────────────────────────────────────────

    /**
     * Ouvre la connexion TCP vers le serveur.
     * @return true si succès, false sinon (RG10)
     */
    public boolean connect() {
        if (isConnected()) return true;   // déjà connecté
        try {
            socket  = new Socket(HOST, PORT);
            out     = new ObjectOutputStream(socket.getOutputStream());
            in      = new ObjectInputStream(socket.getInputStream());
            running = true;
            startListenerThread();
            logger.info("Connected to server {}:{}", HOST, PORT);
            return true;
        } catch (IOException e) {
            logger.error("Cannot connect to server: {}", e.getMessage());
            return false;
        }
    }

    private void startListenerThread() {
        Thread t = new Thread(() -> {
            try {
                while (running) {
                    NetworkMessage msg = (NetworkMessage) in.readObject();
                    if (msg != null && onMessageReceived != null)
                        onMessageReceived.accept(msg);
                }
            } catch (IOException | ClassNotFoundException e) {
                running = false;
                logger.warn("Connection lost: {}", e.getMessage());
                if (onConnectionLost != null) onConnectionLost.run();
            }
        });
        t.setDaemon(true);
        t.setName("ServerListener");
        t.start();
    }

    // ── Envoi ─────────────────────────────────────────────────────

    public synchronized void send(NetworkMessage msg) {
        try {
            out.writeObject(msg);
            out.flush();
            out.reset();
        } catch (IOException e) {
            logger.error("Send failed: {}", e.getMessage());
            if (onConnectionLost != null) onConnectionLost.run();
        }
    }

    // ── Déconnexion ───────────────────────────────────────────────

    public void disconnect() {
        running = false;
        try {
            if (out    != null) out.close();
            if (in     != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
        instance = null;
        logger.info("Disconnected from server");
    }

    // ── Callbacks ─────────────────────────────────────────────────

    public void setOnMessageReceived(Consumer<NetworkMessage> cb) { this.onMessageReceived = cb; }
    public void setOnConnectionLost(Runnable cb)                  { this.onConnectionLost  = cb; }

    // ── État ──────────────────────────────────────────────────────

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}
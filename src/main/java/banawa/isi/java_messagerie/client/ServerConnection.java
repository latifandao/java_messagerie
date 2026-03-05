package banawa.isi.java_messagerie.client;


import banawa.isi.java_messagerie.dao.NetworkMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class ServerConnection {

    private static final Logger logger = LoggerFactory.getLogger(ServerConnection.class);

    private static final String HOST = "localhost";
    private static final int PORT = 5000;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private Thread listenerThread;
    private boolean running = false;

    // Callback — whoever creates this connection provides a handler
    // for incoming messages (e.g. ChatController)
    private Consumer<NetworkMessage> onMessageReceived;

    // Callback for when connection is lost (RG10)
    private Runnable onConnectionLost;

    // --- Singleton so every controller shares the same connection ---

    private static ServerConnection instance;

    public static ServerConnection getInstance() {
        if (instance == null) {
            instance = new ServerConnection();
        }
        return instance;
    }

    private ServerConnection() {}

    // --- Connect ---

    public boolean connect() {
        try {
            socket = new Socket(HOST, PORT);

            // Output before input — always
            out = new ObjectOutputStream(socket.getOutputStream());
            in  = new ObjectInputStream(socket.getInputStream());

            running = true;
            startListenerThread();

            logger.info("Connected to server at {}:{}", HOST, PORT);
            return true;

        } catch (IOException e) {
            logger.error("Could not connect to server: {}", e.getMessage());
            return false;
        }
    }

    // --- Background listener thread ---
    // Constantly reads incoming NetworkMessages and fires the callback

    private void startListenerThread() {
        listenerThread = new Thread(() -> {
            try {
                while (running) {
                    NetworkMessage message = (NetworkMessage) in.readObject();
                    if (message != null && onMessageReceived != null) {
                        onMessageReceived.accept(message);
                    }
                }
            } catch (IOException e) {
                if (running) {
                    // RG10 — connection lost unexpectedly
                    logger.warn("Connection to server lost: {}", e.getMessage());
                    running = false;
                    if (onConnectionLost != null) {
                        onConnectionLost.run();
                    }
                }
            } catch (ClassNotFoundException e) {
                logger.error("Unknown message type received: {}", e.getMessage());
            }
        });

        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    // --- Send a message to the server ---

    public synchronized void send(NetworkMessage message) {
        try {
            out.writeObject(message);
            out.flush();
            out.reset();
        } catch (IOException e) {
            logger.error("Failed to send message: {}", e.getMessage());
            if (onConnectionLost != null) {
                onConnectionLost.run();
            }
        }
    }

    // --- Disconnect ---

    public void disconnect() {
        running = false;
        try {
            if (in != null)     in.close();
            if (out != null)    out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            logger.error("Error disconnecting: {}", e.getMessage());
        }
        instance = null;
        logger.info("Disconnected from server");
    }

    // --- Callbacks ---

    public void setOnMessageReceived(Consumer<NetworkMessage> callback) {
        this.onMessageReceived = callback;
    }

    public void setOnConnectionLost(Runnable callback) {
        this.onConnectionLost = callback;
    }

    // --- State ---

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}
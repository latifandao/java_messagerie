package banawa.isi.java_messagerie.server;


import com.isil3gl.messageriejava.utils.HibernateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    public static final int PORT = 5000;

    public static void main(String[] args) {
        // Trigger Flyway migrations + Hibernate init on startup
        HibernateUtil.getEntityManager().close();
        logger.info("Database initialized successfully");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("Server started on port {}", PORT);

            // Keep accepting new client connections forever (RG11)
            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("New connection from {}", clientSocket.getInetAddress());

                // Spawn a new thread for each client (RG11)
                ClientHandler handler = new ClientHandler(clientSocket);
                Thread thread = new Thread(handler);
                thread.setDaemon(true);
                thread.start();
            }

        } catch (IOException e) {
            logger.error("Server error: {}", e.getMessage());
        }
    }
}
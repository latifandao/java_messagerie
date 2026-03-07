package banawa.isi.java_messagerie;

import banawa.isi.java_messagerie.client.ServerConnection;
import banawa.isi.java_messagerie.network.NetworkMessage;
import banawa.isi.java_messagerie.utils.HibernateUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloApplication1 extends Application {

    private static final Logger logger = LoggerFactory.getLogger(HelloApplication1.class);

    @Override
    public void start(Stage stage) {
        try {
            logger.info("Initializing database...");
            HibernateUtil.getEntityManager().close();
            logger.info("Database ready.");

            FXMLLoader loader = new FXMLLoader(
                    HelloApplication1.class.getResource(
                            "/banawa/isi/java_messagerie/view/FormConnection.fxml"));
            Scene scene = new Scene(loader.load());
            stage.setTitle("Messaging App — Login");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();

        } catch (Exception e) {
            logger.error("Failed to start application: {}", e.getMessage(), e);
            Platform.exit();
        }
    }

    @Override
    public void stop() {
        logger.info("Application shutting down...");
        try {
            ServerConnection connection = ServerConnection.getInstance();
            if (connection.isConnected()) {
                connection.send(new NetworkMessage(NetworkMessage.Type.LOGOUT));
                connection.disconnect();
            }
        } catch (Exception e) {
            logger.warn("Error during disconnect: {}", e.getMessage());
        }
        HibernateUtil.close();
        logger.info("Shutdown complete.");
    }

    public static void main(String[] args) {
        launch();
    }
}
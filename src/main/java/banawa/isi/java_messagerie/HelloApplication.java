package banawa.isi.java_messagerie;


import java.io.IOException;

import banawa.isi.java_messagerie.utils.HibernateUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloApplication extends Application {

    private static final Logger logger = LoggerFactory.getLogger(HelloApplication.class);

    @Override
    public void start(Stage stage) {
        try {
            // Trigger Flyway migrations + Hibernate init before anything opens
            logger.info("Initializing database...");
            HibernateUtil.getEntityManager().close();
            logger.info("Database ready.");

            // Load login screen
            System.out.println(
                    HelloApplication.class.getResource(
                            "/com/isil3gl/messageriejava/view/FormConnection.fxml"));
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource(
                            "/com/isil3gl/messageriejava/view/FormConnection.fxml"));
            Scene scene = new Scene(loader.load());
            stage.setTitle("Messaging App — Login");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();

        } catch (Exception e) {
            logger.error("Failed to start application: {}", e.getMessage());
            e.printStackTrace();
            Platform.exit();
        }
    }

    @Override
    public void stop() {
        // Called when the window is closed — clean up everything
        logger.info("Application shutting down...");

        // Disconnect from server if connected
        try {
            banawa.isi.java_messagerie.client.ServerConnection connection = com.isil3gl.messageriejava.client.ServerConnection
                    .getInstance();
            if (connection.isConnected()) {
                connection.send(new banawa.isi.java_messagerie.network.NetworkMessage(
                        banawa.isi.java_messagerie.network.NetworkMessage.Type.LOGOUT));
                connection.disconnect();
            }
        } catch (Exception e) {
            logger.warn("Error during disconnect on shutdown: {}", e.getMessage());
        }

        // Close Hibernate
        HibernateUtil.close();
        logger.info("Shutdown complete.");
    }

    public static void main(String[] args) {
        launch();
    }
}
// ```
//
// ---
//
// **Your complete final folder structure:**
// ```
// com.isil3gl.messageriejava/
// ├── HelloApplication.java
// ├── client/
// │ └── ServerConnection.java
// ├── controller/
// │ ├── LoginController.java
// │ ├── RegisterController.java
// │ └── ChatController.java
// ├── dao/
// │ ├── UserDAO.java
// │ └── MessageDAO.java
// ├── model/
// │ ├── User.java
// │ └── Message.java
// ├── network/
// │ └── NetworkMessage.java
// ├── server/
// │ ├── Server.java
// │ ├── ClientHandler.java
// │ └── SessionManager.java
// └── utils/
// └── HibernateUtil.java
//
// resources/
// ├── com/isil3gl/messageriejava/
// │ ├── login-view.fxml
// │ ├── register-view.fxml
// │ └── chat-view.fxml
// ├── db.properties
// ├── db/migration/
// │ └── V1__init.sql
// └── META-INF/
// └── persistence.xml
// ```
//
// ---
//
// **How to run the full app:**
//
// **Step 1 — Start the server first:**
//
// Right click `Server.java` → Run. You should see in the console:
// ```
// Database initialized successfully
// Server started on port 5000
// ```
//
// **Step 2 — Start the client:**
//
// Run via Maven:
// ```
// mvn clean javafx:run
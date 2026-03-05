package banawa.isi.java_messagerie.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;



import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import banawa.isi.java_messagerie.client.ServerConnection;
import banawa.isi.java_messagerie.network.NetworkMessage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField usernameTextField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private Button loginButton;
    @FXML private Button registerButton;

    private ServerConnection connection;

    @FXML
    public void initialize() {
        connection = ServerConnection.getInstance();

        // Connect to server when login screen loads
        if (!connection.isConnected()) {
            boolean connected = connection.connect();
            if (!connected) {
                setStatus("Cannot connect to server. Is it running?", true);
                loginButton.setDisable(true);
                registerButton.setDisable(true);
                return;
            }
        }

        // Handle all incoming messages from server
        connection.setOnMessageReceived(message -> {
            Platform.runLater(() -> handleServerMessage(message));
        });

        // RG10 — handle connection lost
        connection.setOnConnectionLost(() -> {
            Platform.runLater(() -> {
                setStatus("Connection to server lost.", true);
                loginButton.setDisable(true);
                registerButton.setDisable(true);
            });
        });
    }

    @FXML
    private void handleLogin() {
        String username = usernameTextField.getText().trim();
        String password = passwordField.getText().trim();

        // Basic client-side validation
        if (username.isEmpty()) {
            setStatus("Please enter a username.", true);
            return;
        }
        if (password.isEmpty()) {
            setStatus("Please enter a password.", true);
            return;
        }

        // Disable buttons while waiting for server response
        loginButton.setDisable(true);
        setStatus("Logging in...", false);

        connection.send(new NetworkMessage(
                NetworkMessage.Type.LOGIN,
                username,
                null,
                password));
    }

    @FXML
    private void goToRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/isil3gl/messageriejava/view/FormInscription.fxml"));
            Stage stage = (Stage) registerButton.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Register");
        } catch (Exception e) {
            setStatus("Could not load register screen.", true);
        }
    }

    private void handleServerMessage(NetworkMessage message) {
        switch (message.getType()) {

            case LOGIN_SUCCESS -> {
                String username = message.getSender();
                openChatWindow(username);
            }

            case LOGIN_FAIL -> {
                setStatus(message.getContent(), true);
                loginButton.setDisable(false);
            }

            default -> {
                // Ignore any other message types on the login screen
            }
        }
    }

    private void openChatWindow(String username) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/isil3gl/messageriejava/view/ChatView.fxml"));
            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(loader.load());
            stage.setScene(scene);
            stage.setTitle("Messaging App — " + username);

            // Pass the logged-in username to the chat controller
            ChatController chatController = loader.getController();
            chatController.initializeChat(username);

        } catch (Exception e) {
            setStatus("Could not load chat window.", true);
            loginButton.setDisable(false);
        }
    }

    // --- Helpers ---

    private void setStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setTextFill(isError
                ? javafx.scene.paint.Color.RED
                : javafx.scene.paint.Color.GREEN);
    }
}


//        **How the login flow works end to end:**
//        ```
//User clicks LOGIN
//      ↓
//Client validates fields not empty
//      ↓
//Sends NetworkMessage(LOGIN, username, null, password) to server
//      ↓
//Server verifies credentials, checks RG3 (not already connected)
//      ↓
//Server sends back LOGIN_SUCCESS or LOGIN_FAIL
//      ↓
//Listener thread receives it → Platform.runLater → handleServerMessage
//      ↓
//LOGIN_SUCCESS → openChatWindow(username)
//LOGIN_FAIL    → show error in statusLabel, re-enable button
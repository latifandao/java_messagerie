package banawa.isi.java_messagerie.controller;

import com.isil3gl.messageriejava.client.ServerConnection;
import com.isil3gl.messageriejava.network.NetworkMessage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class RegisterController {

    @FXML private TextField usernameTextField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label statusLabel;
    @FXML private Button registerButton;
    @FXML private Button backButton;

    private ServerConnection connection;

    @FXML
    public void initialize() {
        connection = ServerConnection.getInstance();

        // Connection should already be open from login screen
        // but handle the case where it isn't
        if (!connection.isConnected()) {
            boolean connected = connection.connect();
            if (!connected) {
                setStatus("Cannot connect to server. Is it running?", true);
                registerButton.setDisable(true);
                return;
            }
        }

        // Handle incoming messages from server
        connection.setOnMessageReceived(message -> {
            Platform.runLater(() -> handleServerMessage(message));
        });

        // RG10 — handle connection lost
        connection.setOnConnectionLost(() -> {
            Platform.runLater(() -> {
                setStatus("Connection to server lost.", true);
                registerButton.setDisable(true);
            });
        });
    }

    @FXML
    private void handleRegister() {
        String username = usernameTextField.getText().trim();
        String password = passwordField.getText().trim();
        String confirm  = confirmPasswordField.getText().trim();

        // Client side validation
        if (username.isEmpty()) {
            setStatus("Please enter a username.", true);
            return;
        }
        if (username.length() > 50) {
            setStatus("Username cannot exceed 50 characters.", true);
            return;
        }
        if (password.isEmpty()) {
            setStatus("Please enter a password.", true);
            return;
        }
        if (password.length() < 6) {
            setStatus("Password must be at least 6 characters.", true);
            return;
        }
        if (!password.equals(confirm)) {
            setStatus("Passwords do not match.", true);
            confirmPasswordField.clear();
            return;
        }

        // Disable button while waiting for server response
        registerButton.setDisable(true);
        setStatus("Creating account...", false);

        connection.send(new NetworkMessage(
                NetworkMessage.Type.REGISTER,
                username,
                null,
                password));
    }

    @FXML
    private void goToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/isil3gl/messageriejava/view/FormConnection.fxml"));
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Login");
        } catch (Exception e) {
            setStatus("Could not load login screen.", true);
        }
    }

    private void handleServerMessage(NetworkMessage message) {
        switch (message.getType()) {

            case REGISTER_SUCCESS -> {
                setStatus("Account created! Redirecting to login...", false);
                // Wait a moment so user can read the success message
                new Thread(() -> {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException ignored) {}
                    Platform.runLater(this::goToLogin);
                }).start();
            }

            case REGISTER_FAIL -> {
                // RG1 — username already taken or other server error
                setStatus(message.getContent(), true);
                registerButton.setDisable(false);
            }

            default -> {
                // Ignore other message types on this screen
            }
        }
    }

    // --- Helpers ---

    private void setStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setTextFill(isError ? Color.RED : Color.GREEN);
    }
}


//        **How the register flow works end to end:**
//        ```
//User fills in username, password, confirm password
//        ↓
//Client validates — not empty, passwords match, length checks
//        ↓
//Sends NetworkMessage(REGISTER, username, null, password) to server
//        ↓
//Server checks RG1 (username unique), hashes password, saves to DB
//        ↓
//Server sends back REGISTER_SUCCESS or REGISTER_FAIL
//        ↓
//REGISTER_SUCCESS → green status, 1.5s pause, redirect to login
//REGISTER_FAIL    → red status (e.g. "Username already taken"), re-enable button
//```
//
//        ---
//
//        **Your controller package now looks like:**
//        ```
//controller/
//        ├── LoginController.java
//└── RegisterController.java
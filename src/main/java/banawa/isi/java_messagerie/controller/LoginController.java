package banawa.isi.java_messagerie.controller;

import banawa.isi.java_messagerie.client.ServerConnection;
import banawa.isi.java_messagerie.network.NetworkMessage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.input.KeyCode;

public class LoginController {

    @FXML private TextField     usernameTextField;
    @FXML private PasswordField passwordField;
    @FXML private Label         statusLabel;
    @FXML private Button        loginButton;
    @FXML private Button        registerButton;

    private ServerConnection connection;

    @FXML
    public void initialize() {
        connection = ServerConnection.getInstance();

        if (!connection.isConnected()) {
            boolean ok = connection.connect();
            if (!ok) {
                setStatus("Impossible de se connecter au serveur.", true);
                loginButton.setDisable(true);
                registerButton.setDisable(true);
                return;
            }
        }
        passwordField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleLogin(); // your login button action method
            }
        });

        connection.setOnMessageReceived(msg ->
                Platform.runLater(() -> handleServerMessage(msg)));

        connection.setOnConnectionLost(() ->
                Platform.runLater(() -> {
                    setStatus("Connexion perdue.", true);
                    loginButton.setDisable(true);
                    registerButton.setDisable(true);
                }));
    }

    @FXML
    private void handleLogin() {
        String username = usernameTextField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty()) { setStatus("Entrez un nom d'utilisateur.", true);  return; }
        if (password.isEmpty()) { setStatus("Entrez un mot de passe.", true);  return; }

        loginButton.setDisable(true);
        setStatus("Connexion…", false);

        connection.send(new NetworkMessage(
                NetworkMessage.Type.LOGIN, username, password));
    }

    @FXML
    private void goToRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/banawa/isi/java_messagerie/view/FormInscription.fxml"));
            Stage stage = (Stage) registerButton.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("creer un compte");
        } catch (Exception e) {
            setStatus("Impossible de charger l'écran d'inscription.", true);
        }
    }

    private void handleServerMessage(NetworkMessage msg) {
        switch (msg.getType()) {

            case LOGIN_SUCCESS -> openChatWindow(msg.getSender());

            case LOGIN_FAIL -> {
                setStatus(msg.getContent(), true);
                loginButton.setDisable(false);
            }

            default -> { /* ignore les autres messages sur cet écran */ }
        }
    }

    private void openChatWindow(String username) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/banawa/isi/java_messagerie/view/ChatView.fxml"));
            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene  = new Scene(loader.load());
            stage.setScene(scene);
            stage.setTitle("Messagerie — " + username);

            ChatController chat = loader.getController();
            chat.initializeChat(username, connection);

        } catch (Exception e) {
            setStatus("Impossible de charger la fenêtre de chat " + e.getMessage(), true);
            loginButton.setDisable(false);
        }
    }

    private void setStatus(String text, boolean error) {
        statusLabel.setText(text);
        statusLabel.setTextFill(error ? Color.RED : Color.GREEN);
    }
}
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

public class RegisterController {

    @FXML private TextField     usernameTextField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label         statusLabel;
    @FXML private Button        registerButton;
    @FXML private Button        backToLoginButton;

    private ServerConnection connection;

    @FXML
    public void initialize() {
        connection = ServerConnection.getInstance();

        passwordField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleRegister(); // your login button action method
            }
        });

        if (!connection.isConnected()) connection.connect();

        connection.setOnMessageReceived(msg ->
                Platform.runLater(() -> handleServerMessage(msg)));
    }

    @FXML
    private void handleRegister() {
        String username = usernameTextField.getText().trim();
        String password = passwordField.getText().trim();
        String confirm  = confirmPasswordField.getText().trim();

        if (username.isEmpty()) { setStatus("Entrez un nom d'utilisateur.", true);          return; }
        if (password.isEmpty()) { setStatus("Entrez un mot de passe.", true);          return; }
        if (!password.equals(confirm)) { setStatus("Les mots de passe ne correspondent pas.", true); return; }

        registerButton.setDisable(true);
        setStatus("Création d'un compte…", false);

        connection.send(new NetworkMessage(
                NetworkMessage.Type.REGISTER, username, password));
    }

    @FXML
    private void goToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/banawa/isi/java_messagerie/view/FormConnection.fxml"));
            Stage stage = (Stage) backToLoginButton.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Se connecter");
        } catch (Exception e) {
            setStatus("Impossible de charger l'écran de connexion.", true);
        }
    }

    private void handleServerMessage(NetworkMessage msg) {
        switch (msg.getType()) {
            case REGISTER_SUCCESS -> {
                setStatus(msg.getContent(), false);
                // Retour automatique vers login après 1.5 s
                new Thread(() -> {
                    try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                    Platform.runLater(this::goToLogin);
                }).start();
            }
            case REGISTER_FAIL -> {
                setStatus(msg.getContent(), true);
                registerButton.setDisable(false);
            }
            default -> {}
        }
    }

    private void setStatus(String text, boolean error) {
        statusLabel.setText(text);
        statusLabel.setTextFill(error ? Color.RED : Color.GREEN);
    }
}
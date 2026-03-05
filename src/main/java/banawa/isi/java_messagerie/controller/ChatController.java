package banawa.isi.java_messagerie.controller;


import banawa.isi.java_messagerie.client.ServerConnection;
import banawa.isi.java_messagerie.model.Message;
import banawa.isi.java_messagerie.network.NetworkMessage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.List;

public class ChatController {

    @FXML private VBox contactListVBox;
    @FXML private VBox scrollbodyChatVBox;
    @FXML private ScrollPane ChatPane;
    @FXML private TextField inputTextField;
    @FXML private Button sendButton;
    @FXML private Label clientNameLabel;

    private ServerConnection connection;
    private String loggedInUsername;
    private String currentChatPartner;

    // --- Called by LoginController after login success ---

    public void initializeChat(String username) {
        this.loggedInUsername = username;
        this.connection = ServerConnection.getInstance();

        // Set message handler
        connection.setOnMessageReceived(message -> {
            Platform.runLater(() -> handleServerMessage(message));
        });

        // RG10 — connection lost
        connection.setOnConnectionLost(() -> {
            Platform.runLater(this::handleConnectionLost);
        });

        // Auto scroll to bottom when new messages arrive
        scrollbodyChatVBox.heightProperty().addListener(
                (obs, oldVal, newVal) -> ChatPane.setVvalue(1.0));

        // Request online users to populate contact list
        connection.send(new NetworkMessage(NetworkMessage.Type.GET_ONLINE_USERS));
    }

    // --- Incoming message router ---

    private void handleServerMessage(NetworkMessage message) {
        switch (message.getType()) {

            case ONLINE_USERS_RESPONSE -> {
                @SuppressWarnings("unchecked")
                List<String> users = (List<String>) message.getData();
                populateContactList(users);
            }

            case RECEIVE_MESSAGE -> {
                String sender  = message.getSender();
                String content = message.getContent();

                // If we're currently chatting with this person show it immediately
                if (sender.equals(currentChatPartner)) {
                    addMessageBubble(content, false);
                } else {
                    // Otherwise highlight their name in the contact list
                    highlightContact(sender);
                }
            }

            case HISTORY_RESPONSE -> {
                @SuppressWarnings("unchecked")
                List<Message> history = (List<Message>) message.getData();
                displayHistory(history);
            }

            case USER_CONNECTED -> {
                String username = message.getContent();
                if (!username.equals(loggedInUsername)) {
                    addContactButton(username);
                }
            }

            case USER_DISCONNECTED -> {
                String username = message.getContent();
                removeContact(username);
            }

            case ERROR -> {
                showErrorInChat(message.getContent());
            }

            default -> {}
        }
    }

    // --- Send message ---

    @FXML
    private void send() {
        if (currentChatPartner == null) {
            showErrorInChat("Select a contact first.");
            return;
        }

        String content = inputTextField.getText().trim();

        // RG7 — client side check
        if (content.isEmpty()) return;
        if (content.length() > 1000) {
            showErrorInChat("Message cannot exceed 1000 characters.");
            return;
        }

        connection.send(new NetworkMessage(
                NetworkMessage.Type.SEND_MESSAGE,
                loggedInUsername,
                currentChatPartner,
                content));

        // Show the message immediately on sender's side
        addMessageBubble(content, true);
        inputTextField.clear();
    }

    // --- Contact list ---

    private void populateContactList(List<String> usernames) {
        contactListVBox.getChildren().clear();
        for (String username : usernames) {
            if (!username.equals(loggedInUsername)) {
                addContactButton(username);
            }
        }
    }

    private void addContactButton(String username) {
        // Don't add duplicates
        boolean exists = contactListVBox.getChildren().stream()
                .anyMatch(node -> node instanceof Button b
                        && b.getText().equals(username));
        if (exists) return;

        Button contactBtn = new Button(username);
        contactBtn.setPrefWidth(170);
        contactBtn.setPrefHeight(45);
        contactBtn.setStyle("""
                -fx-background-color: transparent;
                -fx-border-color: #e0e0e0;
                -fx-border-width: 0 0 1 0;
                -fx-alignment: CENTER_LEFT;
                -fx-padding: 0 0 0 10;
                """);
        contactBtn.setFont(Font.font("Book Antiqua", 14));

        contactBtn.setOnAction(e -> openChat(username));

        // Hover effect
        contactBtn.setOnMouseEntered(e ->
                contactBtn.setStyle("""
                -fx-background-color: #f0f0f0;
                -fx-border-color: #e0e0e0;
                -fx-border-width: 0 0 1 0;
                -fx-alignment: CENTER_LEFT;
                -fx-padding: 0 0 0 10;
                """));
        contactBtn.setOnMouseExited(e ->
                contactBtn.setStyle("""
                -fx-background-color: transparent;
                -fx-border-color: #e0e0e0;
                -fx-border-width: 0 0 1 0;
                -fx-alignment: CENTER_LEFT;
                -fx-padding: 0 0 0 10;
                """));

        contactListVBox.getChildren().add(contactBtn);
    }

    private void removeContact(String username) {
        contactListVBox.getChildren()
                .removeIf(node -> node instanceof Button b
                        && b.getText().equals(username));

        // If we were chatting with them clear the chat
        if (username.equals(currentChatPartner)) {
            currentChatPartner = null;
            clientNameLabel.setText("Select a contact");
            scrollbodyChatVBox.getChildren().clear();
        }
    }

    private void highlightContact(String username) {
        contactListVBox.getChildren().stream()
                .filter(node -> node instanceof Button b
                        && b.getText().equals(username))
                .forEach(node -> node.setStyle("""
                        -fx-background-color: #d4edda;
                        -fx-border-color: #e0e0e0;
                        -fx-border-width: 0 0 1 0;
                        -fx-alignment: CENTER_LEFT;
                        -fx-padding: 0 0 0 10;
                        """));
    }

    // --- Open chat with a contact ---

    private void openChat(String username) {
        currentChatPartner = username;
        clientNameLabel.setText(username);
        scrollbodyChatVBox.getChildren().clear();

        // Reset highlight on this contact
        contactListVBox.getChildren().stream()
                .filter(node -> node instanceof Button b
                        && b.getText().equals(username))
                .forEach(node -> ((Button) node).setStyle("""
                        -fx-background-color: transparent;
                        -fx-border-color: #e0e0e0;
                        -fx-border-width: 0 0 1 0;
                        -fx-alignment: CENTER_LEFT;
                        -fx-padding: 0 0 0 10;
                        """));

        // Request message history from server (RG8)
        NetworkMessage historyRequest = new NetworkMessage(NetworkMessage.Type.GET_HISTORY);
        historyRequest.setSender(loggedInUsername);
        historyRequest.setReceiver(username);
        connection.send(historyRequest);
    }

    // --- Message history display ---

    private void displayHistory(List<Message> history) {
        scrollbodyChatVBox.getChildren().clear();
        for (Message msg : history) {
            boolean isMine = msg.getSender().getUsername().equals(loggedInUsername);
            addMessageBubble(msg.getContenu(), isMine);
        }
    }

    // --- Message bubbles ---

    private void addMessageBubble(String content, boolean isMine) {
        HBox wrapper = new HBox();
        wrapper.setPadding(new Insets(2, 10, 2, 10));

        Label bubble = new Label(content);
        bubble.setWrapText(true);
        bubble.setMaxWidth(400);
        bubble.setPadding(new Insets(8, 12, 8, 12));
        bubble.setFont(Font.font("Book Antiqua", 14));

        if (isMine) {
            // My messages — right side, green
            bubble.setStyle("""
                    -fx-background-color: #84b478;
                    -fx-background-radius: 15 15 0 15;
                    -fx-text-fill: white;
                    """);
            wrapper.setAlignment(Pos.CENTER_RIGHT);
        } else {
            // Their messages — left side, light grey
            bubble.setStyle("""
                    -fx-background-color: #e8e8e8;
                    -fx-background-radius: 15 15 15 0;
                    -fx-text-fill: black;
                    """);
            wrapper.setAlignment(Pos.CENTER_LEFT);
        }

        wrapper.getChildren().add(bubble);
        scrollbodyChatVBox.getChildren().add(wrapper);
    }

    // --- Error display ---

    private void showErrorInChat(String message) {
        Label errorLabel = new Label(message);
        errorLabel.setTextFill(Color.RED);
        errorLabel.setFont(Font.font("Book Antiqua", FontWeight.BOLD, 13));

        HBox wrapper = new HBox(errorLabel);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(new Insets(4));

        scrollbodyChatVBox.getChildren().add(wrapper);
    }

    // --- RG10 — Connection lost ---

    private void handleConnectionLost() {
        try {
            showErrorInChat("Connection to server lost.");
            Thread.sleep(2000);
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/isil3gl/messageriejava/FormConnection.fxml"));
            Stage stage = (Stage) sendButton.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
//```
//
//        ---
//
//        **What `ChatController` handles:**
//
//        | Feature | Method |
//        |---------|--------|
//        | Load online users | `populateContactList` |
//        | User comes online | `addContactButton` |
//        | User goes offline | `removeContact` |
//        | Open conversation | `openChat` → requests history |
//        | Display history | `displayHistory` → `addMessageBubble` |
//        | Send message | `send` |
//        | Receive message | `handleServerMessage` → `addMessageBubble` |
//        | Unread highlight | `highlightContact` |
//        | Connection lost | `handleConnectionLost` → back to login |
//
//        ---
//
//        **Your controller package is now complete:**
//        ```
//controller/
//        ├── LoginController.java
//├── RegisterController.java
//└── ChatController.java

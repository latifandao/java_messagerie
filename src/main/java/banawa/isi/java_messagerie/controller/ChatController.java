
package banawa.isi.java_messagerie.controller;

import banawa.isi.java_messagerie.client.ServerConnection;
import banawa.isi.java_messagerie.network.NetworkMessage;
import banawa.isi.java_messagerie.network.NetworkMessage.UserStatusDTO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ChatController {

    @FXML private VBox       contactListVBox;
    @FXML private VBox       scrollbodyChatVBox;
    @FXML private ScrollPane chatPane;
    @FXML private TextField  inputTextField;
    @FXML private TextField  searchField;
    @FXML private Button     sendButton;
    @FXML private Label      chatPartnerNameLabel;
    @FXML private Label      chatPartnerStatusLabel;
    @FXML private Label      chatPartnerInitialLabel;
    @FXML private Circle     chatPartnerStatusDot;
    @FXML private Label      myUsernameLabel;
    @FXML private Button     logoutButton;

    private ServerConnection connection;
    private String           loggedInUsername;
    private String           currentChatPartner;

    private final Map<String, HBox>    contactRows  = new LinkedHashMap<>();
    private final Map<String, Boolean> onlineStatus = new HashMap<>();
    private final List<String>         allContacts  = new ArrayList<>();

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public void initializeChat(String username, ServerConnection conn) {
        this.loggedInUsername = username;
        this.connection       = conn;

        myUsernameLabel.setText(username);
        chatPartnerNameLabel.setText("Selectionner un contact");
        chatPartnerStatusLabel.setText("");
        chatPartnerInitialLabel.setText("?");
        chatPartnerStatusDot.setFill(Color.web("#bdbdbd"));

        scrollbodyChatVBox.heightProperty().addListener(
                (obs, o, n) -> chatPane.setVvalue(1.0));

        connection.setOnMessageReceived(msg ->
                Platform.runLater(() -> handleServerMessage(msg)));

        connection.setOnConnectionLost(() ->
                Platform.runLater(this::handleConnectionLost));

        connection.send(new NetworkMessage(NetworkMessage.Type.GET_ONLINE_USERS));
    }

    private void handleServerMessage(NetworkMessage msg) {
        switch (msg.getType()) {

            case ONLINE_USERS_RESPONSE -> {
                @SuppressWarnings("unchecked")
                List<UserStatusDTO> users = (List<UserStatusDTO>) msg.getData();
                updateContactList(users);
            }

            case RECEIVE_MESSAGE -> {
                String sender  = msg.getSender();
                String content = msg.getContent();
                if (sender.equals(currentChatPartner)) {
                    addMessageBubble(content, false,
                            msg.getTimestamp() != null ? msg.getTimestamp().format(TIME_FMT) : "");
                } else {
                    markContactUnread(sender);
                }
            }

            case HISTORY_RESPONSE -> {
                @SuppressWarnings("unchecked")
                List<NetworkMessage> history = (List<NetworkMessage>) msg.getData();
                displayHistory(history);
            }

            case USER_STATUS_CHANGE -> {
                String  changedUser = msg.getSender();
                boolean isOnline    = msg.isOnline();
                onlineStatus.put(changedUser, isOnline);

                if (!allContacts.contains(changedUser) && !changedUser.equals(loggedInUsername)) {
                    allContacts.add(changedUser);
                    HBox row = buildContactRow(changedUser, isOnline);
                    contactRows.put(changedUser, row);
                    contactListVBox.getChildren().add(row);
                } else {
                    updateContactStatusDot(changedUser, isOnline);
                }

                if (changedUser.equals(currentChatPartner)) {
                    chatPartnerStatusDot.setFill(isOnline ? Color.web("#25d366") : Color.web("#bdbdbd"));
                    chatPartnerStatusLabel.setText(isOnline ? "Online" : "Offline");
                }
            }

            default -> {}
        }
    }

    // ── Contacts ─────────────────────────────────────────────────

    private void updateContactList(List<UserStatusDTO> users) {
        contactListVBox.getChildren().clear();
        contactRows.clear();
        allContacts.clear();

        for (UserStatusDTO dto : users) {
            String  user     = dto.getUsername();
            boolean isOnline = dto.isOnline();
            onlineStatus.put(user, isOnline);
            allContacts.add(user);
            HBox row = buildContactRow(user, isOnline);
            contactRows.put(user, row);
            contactListVBox.getChildren().add(row);
        }

        if (allContacts.isEmpty()) {
            Label empty = new Label("No other users registered");
            empty.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12; -fx-padding: 20;");
            contactListVBox.getChildren().add(empty);
        }
    }

    private HBox buildContactRow(String username, boolean isOnline) {
        StackPane avatar = new StackPane();
        Circle avatarCircle = new Circle(22);
        avatarCircle.setFill(Color.web("#0d1b2a"));
        Label initial = new Label(username.substring(0, 1).toUpperCase());
        initial.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15;");
        Circle statusDot = new Circle(6);
        statusDot.setId("dot-" + username);
        statusDot.setFill(isOnline ? Color.web("#25d366") : Color.web("#bdbdbd"));
        statusDot.setStroke(Color.WHITE);
        statusDot.setStrokeWidth(1.5);
        StackPane.setAlignment(statusDot, Pos.BOTTOM_RIGHT);
        avatar.getChildren().addAll(avatarCircle, initial, statusDot);

        VBox info = new VBox(2);
        Label nameLabel = new Label(username);
        nameLabel.setFont(Font.font("System", 14));
        // ✅ CORRECTION : couleur du nom en noir visible
        nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1a1a1a;");
        Label statusText = new Label(isOnline ? "en ligne" : "hors line");
        statusText.setId("status-" + username);
        statusText.setStyle("-fx-font-size: 11; -fx-text-fill: "
                + (isOnline ? "#25d366" : "#aaaaaa") + ";");
        info.getChildren().addAll(nameLabel, statusText);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label badge = new Label("");
        badge.setId("badge-" + username);
        badge.setVisible(false);
        badge.setStyle("-fx-background-color: #25d366; -fx-background-radius: 10;"
                + "-fx-text-fill: white; -fx-padding: 2 7 2 7; -fx-font-size: 11; -fx-font-weight: bold;");

        HBox row = new HBox(12, avatar, info, badge);
        row.setPadding(new Insets(10, 15, 10, 15));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-cursor: hand; -fx-border-color: transparent transparent #f0f0f0 transparent;");
        row.setOnMouseClicked(e -> openChat(username));
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #f5f5f5; -fx-cursor: hand;"
                + "-fx-border-color: transparent transparent #f0f0f0 transparent;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-cursor: hand;"
                + "-fx-border-color: transparent transparent #f0f0f0 transparent;"));
        return row;
    }

    private void updateContactStatusDot(String username, boolean isOnline) {
        contactListVBox.lookupAll("#dot-" + username).forEach(node -> {
            if (node instanceof Circle c)
                c.setFill(isOnline ? Color.web("#25d366") : Color.web("#bdbdbd"));
        });
        contactListVBox.lookupAll("#status-" + username).forEach(node -> {
            if (node instanceof Label l) {
                l.setText(isOnline ? "Online" : "Offline");
                l.setStyle("-fx-font-size: 11; -fx-text-fill: " + (isOnline ? "#25d366" : "#aaaaaa") + ";");
            }
        });
    }

    private void markContactUnread(String username) {
        contactListVBox.lookupAll("#badge-" + username).forEach(node -> {
            if (node instanceof Label badge) {
                badge.setVisible(true);
                try {
                    int count = badge.getText().isEmpty() ? 0 : Integer.parseInt(badge.getText());
                    badge.setText(String.valueOf(count + 1));
                } catch (NumberFormatException e) {
                    badge.setText("1");
                }
            }
        });
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim().toLowerCase();
        contactListVBox.getChildren().clear();

        List<String> filtered = allContacts.stream()
                .filter(u -> u.toLowerCase().contains(query))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            Label empty = new Label(query.isEmpty() ? "No contacts available"
                    : "No results for \"" + query + "\"");
            empty.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12; -fx-padding: 20;");
            contactListVBox.getChildren().add(empty);
        } else {
            for (String user : filtered) {
                HBox row = contactRows.get(user);
                if (row != null) contactListVBox.getChildren().add(row);
            }
        }
    }

    private void openChat(String username) {
        currentChatPartner = username;
        chatPartnerNameLabel.setText(username);
        chatPartnerInitialLabel.setText(username.substring(0, 1).toUpperCase());

        boolean isOnline = onlineStatus.getOrDefault(username, false);
        chatPartnerStatusDot.setFill(isOnline ? Color.web("#25d366") : Color.web("#bdbdbd"));
        chatPartnerStatusLabel.setText(isOnline ? "Online" : "Offline");

        scrollbodyChatVBox.getChildren().clear();

        contactListVBox.lookupAll("#badge-" + username).forEach(node -> {
            if (node instanceof Label badge) { badge.setText(""); badge.setVisible(false); }
        });

        contactRows.forEach((user, row) -> {
            if (user.equals(username)) {
                row.setStyle("-fx-background-color: #e8f5e9; -fx-cursor: hand;"
                        + "-fx-border-color: transparent transparent #f0f0f0 transparent;");
            } else {
                row.setStyle("-fx-cursor: hand;"
                        + "-fx-border-color: transparent transparent #f0f0f0 transparent;");
            }
        });

        connection.send(new NetworkMessage(NetworkMessage.Type.GET_HISTORY,
                loggedInUsername, username, null));
    }

    private void displayHistory(List<NetworkMessage> history) {
        scrollbodyChatVBox.getChildren().clear();
        for (NetworkMessage m : history) {
            boolean mine = m.getSender().equals(loggedInUsername);
            String time  = m.getTimestamp() != null ? m.getTimestamp().format(TIME_FMT) : "";
            addMessageBubble(m.getContent(), mine, time);
        }
    }

    @FXML
    private void handleSend() {
        if (currentChatPartner == null) {
            chatPartnerStatusLabel.setText("Select a contact first!");
            return;
        }
        String content = inputTextField.getText().trim();
        if (content.isEmpty() || content.length() > 1000) return;

        connection.send(new NetworkMessage(NetworkMessage.Type.SEND_MESSAGE,
                loggedInUsername, currentChatPartner, content));
        addMessageBubble(content, true, java.time.LocalTime.now().format(TIME_FMT));
        inputTextField.clear();
    }

    private void addMessageBubble(String content, boolean isMine, String time) {
        HBox wrapper = new HBox();
        wrapper.setPadding(new Insets(3, 12, 3, 12));

        VBox bubble = new VBox(4);
        bubble.setMaxWidth(420);
        bubble.setPadding(new Insets(9, 14, 9, 14));

        Label contentLabel = new Label(content);
        contentLabel.setWrapText(true);
        contentLabel.setFont(Font.font("System", 14));
        // ✅ CORRECTION : texte toujours noir et visible
        contentLabel.setStyle("-fx-text-fill: #1a1a1a;");

        Label timeLabel = new Label(time);
        timeLabel.setFont(Font.font(10));
        timeLabel.setTextFill(Color.web("#888"));
        // ✅ CORRECTION : heure aussi en couleur visible
        timeLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 10;");

        bubble.getChildren().addAll(contentLabel, timeLabel);

        if (isMine) {
            // ✅ Message envoyé : bulle verte claire, texte noir
            bubble.setStyle("-fx-background-color: #dcf8c6; -fx-background-radius: 15 15 0 15;");
            timeLabel.setAlignment(Pos.CENTER_RIGHT);
            wrapper.setAlignment(Pos.CENTER_RIGHT);
        } else {
            // ✅ Message reçu : bulle blanche, texte noir
            bubble.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 0 15 15 15;"
                    + "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.08),4,0,0,1);");
            wrapper.setAlignment(Pos.CENTER_LEFT);
        }

        wrapper.getChildren().add(bubble);
        scrollbodyChatVBox.getChildren().add(wrapper);
    }

    @FXML
    private void handleLogout() {
        connection.send(new NetworkMessage(NetworkMessage.Type.LOGOUT));
        connection.disconnect();
        openLoginScreen();
    }

    private void handleConnectionLost() {
        Alert alert = new Alert(Alert.AlertType.ERROR,
                "Connection to server lost. (RG10)", ButtonType.OK);
        alert.setTitle("Connection Error");
        alert.showAndWait();
        openLoginScreen();
    }

    private void openLoginScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/banawa/isi/java_messagerie/view/FormConnection.fxml"));
            Stage stage = (Stage) sendButton.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
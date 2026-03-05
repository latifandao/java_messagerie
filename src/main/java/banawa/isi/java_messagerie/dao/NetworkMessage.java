package banawa.isi.java_messagerie.dao;

import java.io.Serializable;

public class NetworkMessage implements Serializable {

    public enum Type {
        // Auth
        LOGIN,
        REGISTER,
        LOGOUT,

        // Messaging
        SEND_MESSAGE,
        RECEIVE_MESSAGE,
        GET_HISTORY,
        HISTORY_RESPONSE,

        // Users
        GET_ONLINE_USERS,
        ONLINE_USERS_RESPONSE,
        USER_CONNECTED,
        USER_DISCONNECTED,

        // Pending messages delivered on login (RG6)
        PENDING_MESSAGES,

        // Responses
        SUCCESS,
        ERROR
    }

    private Type type;
    private String senderUsername;
    private String receiverUsername;
    private String content;
    private Object data; // flexible payload (List<Message>, List<User>, etc.)

    // --- Constructors ---

    public NetworkMessage() {}

    public NetworkMessage(Type type) {
        this.type = type;
    }

    public NetworkMessage(Type type, String senderUsername,
                          String receiverUsername, String content) {
        this.type = type;
        this.senderUsername = senderUsername;
        this.receiverUsername = receiverUsername;
        this.content = content;
    }

    // --- Getters & Setters ---

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }

    public String getReceiverUsername() { return receiverUsername; }
    public void setReceiverUsername(String receiverUsername) { this.receiverUsername = receiverUsername; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    @Override
    public String toString() {
        return "NetworkMessage{type=" + type +
                ", from=" + senderUsername +
                ", to=" + receiverUsername + "}";
    }
}
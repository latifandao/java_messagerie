package banawa.isi.java_messagerie.network;


import java.io.Serial;
import java.io.Serializable;

public class NetworkMessage implements Serializable {

    // Serial version — important for socket serialization stability
    @Serial
    private static final long serialVersionUID = 1L;

    // Every possible message type between client and server
    public enum Type {
        // Auth
        REGISTER,
        REGISTER_SUCCESS,
        REGISTER_FAIL,

        LOGIN,
        LOGIN_SUCCESS,
        LOGIN_FAIL,

        LOGOUT,

        // Messaging
        SEND_MESSAGE,
        RECEIVE_MESSAGE,

        // History & users
        GET_HISTORY,
        HISTORY_RESPONSE,

        GET_ONLINE_USERS,
        ONLINE_USERS_RESPONSE,

        // Status updates pushed by server to all clients
        USER_CONNECTED,
        USER_DISCONNECTED,

        // Errors
        ERROR
    }

    private Type type;        // what kind of message this is
    private String sender;    // username of sender
    private String receiver;  // username of receiver
    private String content;   // message text, error text, or any string payload
    private Object data;      // flexible payload — List<Message>, List<String>, etc.

    // --- Constructors ---

    public NetworkMessage() {}

    // Simple type-only message (e.g. LOGOUT, GET_ONLINE_USERS)
    public NetworkMessage(Type type) {
        this.type = type;
    }

    // Type + single string payload (e.g. ERROR with a description)
    public NetworkMessage(Type type, String content) {
        this.type = type;
        this.content = content;
    }

    // Full message (e.g. SEND_MESSAGE)
    public NetworkMessage(Type type, String sender, String receiver, String content) {
        this.type = type;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
    }

    // Type + data payload (e.g. HISTORY_RESPONSE with a List<Message>)
    public NetworkMessage(Type type, Object data) {
        this.type = type;
        this.data = data;
    }

    // --- Getters & Setters ---

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    @Override
    public String toString() {
        return "NetworkMessage{type=" + type +
                ", sender='" + sender + "'" +
                ", receiver='" + receiver + "'" +
                ", content='" + content + "'}";
    }
}

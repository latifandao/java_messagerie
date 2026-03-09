package banawa.isi.java_messagerie.network;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Classe unique de message réseau — utilisée par client ET serveur.
 * NE PAS dupliquer dans d'autres packages.
 */
public class NetworkMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    // ── UserStatusDTO intégré ──────────────────────────────────────

    /**
     * DTO pour envoyer un contact avec son statut online/offline.
     * Intégré ici pour éviter tout problème d'import entre client et serveur.
     */
    public static class UserStatusDTO implements Serializable {
        private static final long serialVersionUID = 2L;

        private String  username;
        private boolean online;

        public UserStatusDTO() {}

        public UserStatusDTO(String username, boolean online) {
            this.username = username;
            this.online   = online;
        }

        public String  getUsername() { return username; }
        public boolean isOnline()    { return online; }

        @Override
        public String toString() {
            return username + " [" + (online ? "ONLINE" : "OFFLINE") + "]";
        }
    }

    // ── Type ──────────────────────────────────────────────────────

    public enum Type {
        LOGIN, REGISTER, LOGOUT,
        LOGIN_SUCCESS, LOGIN_FAIL,
        REGISTER_SUCCESS, REGISTER_FAIL,
        SEND_MESSAGE, RECEIVE_MESSAGE,
        GET_ONLINE_USERS, ONLINE_USERS_RESPONSE,
        USER_STATUS_CHANGE,
        GET_HISTORY, HISTORY_RESPONSE,
        ERROR
    }

    // ── Champs ────────────────────────────────────────────────────

    private Type          type;
    private String        sender;
    private String        receiver;
    private String        content;
    private LocalDateTime timestamp;
    private Object        data;
    private boolean       online;

    // ── Constructeurs ─────────────────────────────────────────────

    public NetworkMessage() {}

    public NetworkMessage(Type type) {
        this.type      = type;
        this.timestamp = LocalDateTime.now();
    }

    public NetworkMessage(Type type, String sender, String content) {
        this.type      = type;
        this.sender    = sender;
        this.content   = content;
        this.timestamp = LocalDateTime.now();
    }

    public NetworkMessage(Type type, String sender, String receiver, String content) {
        this.type      = type;
        this.sender    = sender;
        this.receiver  = receiver;
        this.content   = content;
        this.timestamp = LocalDateTime.now();
    }

    public NetworkMessage(Type type, Object data) {
        this.type      = type;
        this.data      = data;
        this.timestamp = LocalDateTime.now();
    }

    // ── Méthodes statiques ────────────────────────────────────────

    public static NetworkMessage statusChange(String username, boolean online) {
        NetworkMessage msg = new NetworkMessage();
        msg.type      = Type.USER_STATUS_CHANGE;
        msg.sender    = username;
        msg.online    = online;
        msg.timestamp = LocalDateTime.now();
        return msg;
    }

    public static NetworkMessage loginSuccess(String username) {
        NetworkMessage msg = new NetworkMessage();
        msg.type      = Type.LOGIN_SUCCESS;
        msg.sender    = username;
        msg.timestamp = LocalDateTime.now();
        return msg;
    }

    public static NetworkMessage fail(Type type, String reason) {
        NetworkMessage msg = new NetworkMessage();
        msg.type      = type;
        msg.content   = reason;
        msg.timestamp = LocalDateTime.now();
        return msg;
    }

    // ── Getters / Setters ─────────────────────────────────────────

    public Type getType() {
        return type;
    }
    public void          setType(Type type)                { this.type = type; }
    public String        getSender()                       { return sender; }
    public void          setSender(String sender)          { this.sender = sender; }
    public String        getReceiver()                     { return receiver; }
    public void          setReceiver(String receiver)      { this.receiver = receiver; }
    public String        getContent()                      { return content; }
    public void          setContent(String content)        { this.content = content; }
    public LocalDateTime getTimestamp()                    { return timestamp; }
    public void          setTimestamp(LocalDateTime ts)    { this.timestamp = ts; }
    public Object        getData()                         { return data; }
    public void          setData(Object data)              { this.data = data; }
    public boolean       isOnline()                        { return online; }
    public void          setOnline(boolean online)         { this.online = online; }

    @Override
    public String toString() {
        return "NetworkMessage{type=" + type + ", sender=" + sender
                + ", receiver=" + receiver + ", content=" + content + "}";
    }
}
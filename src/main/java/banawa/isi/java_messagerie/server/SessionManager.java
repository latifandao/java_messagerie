package banawa.isi.java_messagerie.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    // Thread-safe map of username -> their ClientHandler
    private static final ConcurrentHashMap<String, ClientHandler> activeSessions
            = new ConcurrentHashMap<>();

    // --- Session management ---

    public static void addSession(String username, ClientHandler handler) {
        activeSessions.put(username, handler);
    }

    public static void removeSession(String username) {
        activeSessions.remove(username);
    }

    public static boolean isOnline(String username) {
        return activeSessions.containsKey(username);
    }

    // RG3 — check before allowing login
    public static boolean isAlreadyConnected(String username) {
        return activeSessions.containsKey(username);
    }

    // Get a specific client's handler to send them a message directly
    public static ClientHandler getHandler(String username) {
        return activeSessions.get(username);
    }

    // Get list of all online usernames — for GET_ONLINE_USERS response
    public static List<String> getOnlineUsernames() {
        return new ArrayList<>(activeSessions.keySet());
    }

    // Broadcast a message to every connected client except one
    public static void broadcastExcept(String excludeUsername,
                                       com.isil3gl.messageriejava.network.NetworkMessage message) {
        activeSessions.forEach((username, handler) -> {
            if (!username.equals(excludeUsername)) {
                handler.sendMessage(message);
            }
        });
    }

    // Broadcast to everyone including sender
    public static void broadcastAll(com.isil3gl.messageriejava.network.NetworkMessage message) {
        activeSessions.forEach((username, handler) -> handler.sendMessage(message));
    }

    public static int getOnlineCount() {
        return activeSessions.size();
    }
}
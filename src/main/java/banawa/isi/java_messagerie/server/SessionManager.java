package banawa.isi.java_messagerie.server;

import banawa.isi.java_messagerie.network.NetworkMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire de sessions — délègue à Server.java.
 * Gardé pour compatibilité mais Server.java est la source de vérité.
 */
public class SessionManager {

    private static final ConcurrentHashMap<String, ClientHandler> activeSessions
            = new ConcurrentHashMap<>();

    public static void addSession(String username, ClientHandler handler) {
        activeSessions.put(username, handler);
    }

    public static void removeSession(String username) {
        activeSessions.remove(username);
    }

    public static boolean isOnline(String username) {
        return activeSessions.containsKey(username);
    }

    public static boolean isAlreadyConnected(String username) {
        return activeSessions.containsKey(username);
    }

    public static ClientHandler getHandler(String username) {
        return activeSessions.get(username);
    }

    public static List<String> getOnlineUsernames() {
        return new ArrayList<>(activeSessions.keySet());
    }

    public static void broadcastExcept(String excludeUsername, NetworkMessage message) {
        activeSessions.forEach((username, handler) -> {
            if (!username.equals(excludeUsername)) {
                handler.safeSend(message);   // safeSend au lieu de sendMessage
            }
        });
    }

    public static void broadcast(NetworkMessage message) {
        activeSessions.forEach((username, handler) ->
                handler.safeSend(message));  // safeSend au lieu de sendMessage
    }

    public static int getOnlineCount() {
        return activeSessions.size();
    }
}
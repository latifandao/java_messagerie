package banawa.isi.java_messagerie.dao;

import banawa.isi.java_messagerie.model.Message;
import banawa.isi.java_messagerie.utils.HibernateUtil;
import jakarta.persistence.EntityManager;

import java.util.List;

public class MessageDAO {

    // --- Create ---

    public void save(Message message) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(message);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw new RuntimeException("Error saving message", e);
        } finally {
            em.close();
        }
    }

    // --- Read ---

    // Get full conversation between two users, ordered by date (RG8)
    public List<Message> getConversation(String user1, String user2) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.createQuery("""
                    SELECT m FROM Message m
                    WHERE (m.sender.username = :user1 AND m.receiver.username = :user2)
                       OR (m.sender.username = :user2 AND m.receiver.username = :user1)
                    ORDER BY m.dateEnvoi ASC
                    """, Message.class)
                    .setParameter("user1", user1)
                    .setParameter("user2", user2)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // Get messages that were sent to a user while they were offline (RG6)
    public List<Message> getPendingMessages(String username) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.createQuery("""
                    SELECT m FROM Message m
                    WHERE m.receiver.username = :username
                    AND m.statut = :statut
                    ORDER BY m.dateEnvoi ASC
                    """, Message.class)
                    .setParameter("username", username)
                    .setParameter("statut", Message.MessageStatus.ENVOYE)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // --- Update ---

    public void updateStatus(Long messageId, Message.MessageStatus status) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery(
                            "UPDATE Message m SET m.statut = :statut WHERE m.id = :id")
                    .setParameter("statut", status)
                    .setParameter("id", messageId)
                    .executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw new RuntimeException("Error updating message status", e);
        } finally {
            em.close();
        }
    }

    // Mark all messages in a conversation as read
    public void markConversationAsRead(String senderUsername, String receiverUsername) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("""
                    UPDATE Message m SET m.statut = :statut
                    WHERE m.sender.username = :sender
                    AND m.receiver.username = :receiver
                    AND m.statut != :alreadyRead
                    """)
                    .setParameter("statut", Message.MessageStatus.LU)
                    .setParameter("sender", senderUsername)
                    .setParameter("receiver", receiverUsername)
                    .setParameter("alreadyRead", Message.MessageStatus.LU)
                    .executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw new RuntimeException("Error marking messages as read", e);
        } finally {
            em.close();
        }
    }
}

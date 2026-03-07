package banawa.isi.java_messagerie.dao;

import banawa.isi.java_messagerie.model.Message;
import banawa.isi.java_messagerie.utils.HibernateUtil;
import jakarta.persistence.EntityManager;

import java.util.List;

public class MessageDAO {

    public void save(Message message) {
        try (EntityManager em = HibernateUtil.getEntityManager()) {
            em.getTransaction().begin();
            em.persist(message);
            em.getTransaction().commit();
        }
    }

    public void update(Message message) {
        try (EntityManager em = HibernateUtil.getEntityManager()) {
            em.getTransaction().begin();
            em.merge(message);
            em.getTransaction().commit();
        }
    }

    /**
     * Messages non encore livrés (ENVOYE) pour un destinataire donné.
     * Utilisé pour livrer les messages hors-ligne à la reconnexion.
     */
    public List<Message> getUndeliveredMessages(String receiverUsername) {
        try (EntityManager em = HibernateUtil.getEntityManager()) {
            return em.createQuery(
                            "SELECT m FROM Message m " +
                                    "WHERE m.receiver.username = :username " +
                                    "AND m.statut = 'ENVOYE' " +
                                    "ORDER BY m.dateEnvoi ASC", Message.class)
                    .setParameter("username", receiverUsername)
                    .getResultList();
        }
    }

    /**
     * Historique complet entre 2 utilisateurs (ordre chronologique — RG8).
     */
    public List<Message> getHistory(String userA, String userB) {
        try (EntityManager em = HibernateUtil.getEntityManager()) {
            return em.createQuery(
                            "SELECT m FROM Message m " +
                                    "WHERE (m.sender.username = :a AND m.receiver.username = :b) " +
                                    "   OR (m.sender.username = :b AND m.receiver.username = :a) " +
                                    "ORDER BY m.dateEnvoi ASC", Message.class)
                    .setParameter("a", userA)
                    .setParameter("b", userB)
                    .getResultList();
        }
    }

    public void markAsDelivered(Message message) {
        try (EntityManager em = HibernateUtil.getEntityManager()) {
            em.getTransaction().begin();
            em.createQuery(
                            "UPDATE Message m SET m.statut = 'RECU' WHERE m.id = :id")
                    .setParameter("id", message.getId())
                    .executeUpdate();
            em.getTransaction().commit();
        }
    }

    public void markAsRead(Long messageId) {
        try (EntityManager em = HibernateUtil.getEntityManager()) {
            em.getTransaction().begin();
            em.createQuery(
                            "UPDATE Message m SET m.statut = 'LU' WHERE m.id = :id")
                    .setParameter("id", messageId)
                    .executeUpdate();
            em.getTransaction().commit();
        }
    }
}
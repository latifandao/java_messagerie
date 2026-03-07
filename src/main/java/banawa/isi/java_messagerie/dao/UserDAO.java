package banawa.isi.java_messagerie.dao;

import banawa.isi.java_messagerie.model.User;
import banawa.isi.java_messagerie.utils.HibernateUtil;
import jakarta.persistence.EntityManager;

import java.util.List;

public class UserDAO {

    public void save(User user) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(user);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public User findByUsername(String username) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            List<User> results = em.createQuery(
                            "FROM User WHERE username = :username", User.class)
                    .setParameter("username", username)
                    .getResultList();
            return results.isEmpty() ? null : results.get(0);
        } finally {
            em.close();
        }
    }

    public boolean usernameExists(String username) {
        return findByUsername(username) != null;
    }

    public void updateStatus(String username, User.Status status) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("UPDATE User SET status = :status WHERE username = :username")
                    .setParameter("status", status)
                    .setParameter("username", username)
                    .executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    // ✅ NOUVELLE MÉTHODE : récupère tous les utilisateurs enregistrés
    public List<User> findAll() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.createQuery("FROM User", User.class).getResultList();
        } finally {
            em.close();
        }
    }
}
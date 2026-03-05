package banawa.isi.java_messagerie.utils;


import banawa.isi.java_messagerie.model.Message;
import banawa.isi.java_messagerie.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {

    private static final String URL      = "jdbc:postgresql://localhost:5432/messaging_db";
    private static final String USER     = "postgres";
    private static final String PASSWORD = "passer";

    private static final EntityManagerFactory emf;

    static {
        // Step 1 — Run Flyway migrations
        Flyway flyway = Flyway.configure()
                .dataSource(URL, USER, PASSWORD)
                .locations("filesystem:src/main/resources/db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();
        flyway.migrate();

        // Step 2 — Configure Hibernate programmatically (no persistence.xml needed)
        Configuration config = new Configuration();

        // Connection settings
        config.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
        config.setProperty("hibernate.connection.url",      URL);
        config.setProperty("hibernate.connection.username", USER);
        config.setProperty("hibernate.connection.password", PASSWORD);

        // Dialect
        config.setProperty("hibernate.dialect",
                "org.hibernate.dialect.PostgreSQLDialect");

        // Schema — validate since Flyway already created the tables
        config.setProperty("hibernate.hbm2ddl.auto", "validate");

        // Logging
        config.setProperty("hibernate.show_sql",   "true");
        config.setProperty("hibernate.format_sql", "true");

        // Register entity classes manually
        config.addAnnotatedClass(User.class);
        config.addAnnotatedClass(Message.class);

        emf = config.buildSessionFactory().unwrap(EntityManagerFactory.class);
    }

    public static EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public static void close() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}
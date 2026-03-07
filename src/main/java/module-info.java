module banawa.isi.java_messagerie {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires java.naming;
    requires java.sql;
    requires org.postgresql.jdbc;
    requires bcrypt;
    requires org.slf4j;
    requires flyway.core;

    opens banawa.isi.java_messagerie to javafx.fxml;
    opens banawa.isi.java_messagerie.controller to javafx.fxml;
    opens banawa.isi.java_messagerie.model to org.hibernate.orm.core;
    opens banawa.isi.java_messagerie.view to javafx.fxml;

    exports banawa.isi.java_messagerie;
    exports banawa.isi.java_messagerie.model;
    exports banawa.isi.java_messagerie.utils;
}
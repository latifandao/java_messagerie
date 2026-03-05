module banawa.isi.java_messagerie {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.ikonli.javafx;

    opens banawa.isi.java_messagerie to javafx.fxml;
    exports banawa.isi.java_messagerie;

}

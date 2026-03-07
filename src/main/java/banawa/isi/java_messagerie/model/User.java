package banawa.isi.java_messagerie.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Status { ONLINE, OFFLINE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OFFLINE;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    // Constructeurs
    public User() {}

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.status = Status.OFFLINE;
        this.dateCreation = LocalDateTime.now();
    }

    // Getters / Setters
    public Long getId()                          { return id; }
    public String getUsername()                  { return username; }
    public void setUsername(String username)     { this.username = username; }
    public String getPassword()                  { return password; }
    public void setPassword(String password)     { this.password = password; }
    public Status getStatus()                    { return status; }
    public void setStatus(Status status)         { this.status = status; }
    public LocalDateTime getDateCreation()       { return dateCreation; }
    public void setDateCreation(LocalDateTime d) { this.dateCreation = d; }
}
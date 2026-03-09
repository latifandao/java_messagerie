package banawa.isi.java_messagerie.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Statut { ENVOYE, RECU, LU }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(nullable = false, length = 1000)
    private String contenu;

    @Column(name = "date_envoi", nullable = false)
    private LocalDateTime dateEnvoi = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.ENVOYE;

    // Constructeurs
    public Message() {}

    public Message(User sender, User receiver, String contenu) {
        this.sender = sender;
        this.receiver = receiver;
        this.contenu = contenu;
        this.dateEnvoi = LocalDateTime.now();
        this.statut = Statut.ENVOYE;
    }

    // Getters / Setters
    public Long getId()                          { return id; }
    public User getSender()                      { return sender; }
    public void setSender(User sender)           { this.sender = sender; }
    public User getReceiver()                    { return receiver; }
    public void setReceiver(User receiver)       { this.receiver = receiver; }
    public String getContenu()                   { return contenu; }
    public void setContenu(String contenu)       { this.contenu = contenu; }
    public LocalDateTime getDateEnvoi()          { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime d)    { this.dateEnvoi = d; }
    public Statut getStatut()                    { return statut; }
    public void setStatut(Statut statut)         { this.statut = statut; }
}
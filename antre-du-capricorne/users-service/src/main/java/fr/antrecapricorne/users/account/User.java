package fr.antrecapricorne.users.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Un compte utilisateur : correspond à la table {@code users} (migration V1).
 *
 * <p>Hibernate est réglé sur {@code ddl-auto: validate} : il vérifie au démarrage que cette classe
 * et la table sont d'accord, mais ne modifie jamais la base. Le schéma appartient à Flyway.</p>
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "pseudo", nullable = false, length = 30)
    private String pseudo;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    // Rempli par la base (DEFAULT now()) : jamais écrit par l'application.
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    /** Constructeur exigé par JPA. */
    protected User() {
    }

    public User(UUID id, String pseudo, String email, String passwordHash, Role role) {
        this.id = id;
        this.pseudo = pseudo;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.enabled = true;
    }

    public UUID getId() {
        return id;
    }

    public String getPseudo() {
        return pseudo;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Désactive le compte (utilisé plus tard par l'administration). */
    public void disable() {
        this.enabled = false;
    }
}

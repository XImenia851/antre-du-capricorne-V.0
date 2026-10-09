package fr.antrecapricorne.users.account;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Logique métier des comptes : inscription et vérification des identifiants.
 *
 * <p>Pas de {@code @Transactional} englobant sur {@link #register} : {@code saveAndFlush} ouvre sa
 * propre transaction et l'erreur de doublon (contrainte unique) remonte ici, où on peut l'attraper
 * proprement. Dans une transaction englobante, l'erreur n'apparaîtrait qu'au commit, trop tard.</p>
 */
@Service
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder encoder;

    /**
     * Hash factice, calculé une fois au démarrage. Il sert à « perdre le même temps » quand
     * l'e-mail est inconnu : sans cela, une réponse rapide révélerait que le compte n'existe pas
     * (attaque par chronométrage).
     */
    private final String dummyHash;

    public UserService(UserRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
        this.dummyHash = encoder.encode("mot-de-passe-factice-pour-egaliser-le-temps");
    }

    public User register(String pseudo, String email, String rawPassword) {
        if (!ValidationRules.fitsBcrypt(rawPassword)) {
            throw new InvalidPasswordException();
        }
        String normalizedEmail = email.trim().toLowerCase();
        String cleanPseudo = pseudo.trim();

        if (repository.existsByPseudo(cleanPseudo) || repository.existsByEmail(normalizedEmail)) {
            throw new DuplicateUserException();
        }

        // Le rôle est imposé ici, jamais lu depuis la requête (anti « mass assignment »).
        User user = new User(UUID.randomUUID(), cleanPseudo, normalizedEmail,
                encoder.encode(rawPassword), Role.USER);
        try {
            return repository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Deux inscriptions simultanées : les index uniques de la base tranchent.
            throw new DuplicateUserException();
        }
    }

    /** Retourne le compte si l'e-mail et le mot de passe correspondent et que le compte est actif. */
    public Optional<User> verify(String email, String rawPassword) {
        Optional<User> found = repository.findByEmail(email.trim());
        String hashToCheck = found.map(User::getPasswordHash).orElse(dummyHash);

        // Le calcul BCrypt a TOUJOURS lieu, même si le compte n'existe pas ou est désactivé.
        boolean matches = ValidationRules.fitsBcrypt(rawPassword) && encoder.matches(rawPassword, hashToCheck);

        return found.filter(u -> matches && u.isEnabled());
    }
}

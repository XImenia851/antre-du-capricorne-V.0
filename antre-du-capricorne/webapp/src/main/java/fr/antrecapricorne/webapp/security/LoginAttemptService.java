package fr.antrecapricorne.webapp.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite les tentatives de connexion ratées (contre les essais de mots de passe en boucle).
 *
 * <p>Principe : après {@code maxFailures} échecs pour une même clé (adresse IP + e-mail), la
 * connexion est refusée jusqu'à la fin de la fenêtre de temps, MÊME avec le bon mot de passe.
 * La clé combine l'IP et l'e-mail pour qu'un inconnu ne puisse pas bloquer le compte de quelqu'un
 * d'autre depuis n'importe où. La clé existe aussi pour les e-mails qui n'ont aucun compte : on ne
 * révèle donc pas quels comptes existent.</p>
 *
 * <p>Limite assumée : le compteur est en mémoire (perdu au redémarrage, non partagé entre plusieurs
 * instances). C'est suffisant pour une seule instance ; on pourra passer sur Redis plus tard.</p>
 */
public class LoginAttemptService {

    private static final int PURGE_THRESHOLD = 10_000;
    private static final int HARD_CAP = 50_000;

    private record Entry(int failures, Instant windowStart) {
    }

    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final Duration window;
    private final Clock clock;

    public LoginAttemptService(int maxFailures, Duration window, Clock clock) {
        this.maxFailures = maxFailures;
        this.window = window;
        this.clock = clock;
    }

    public boolean isBlocked(String key) {
        Entry entry = entries.get(key);
        if (entry == null) {
            return false;
        }
        if (isExpired(entry)) {
            entries.remove(key, entry);
            return false;
        }
        return entry.failures() >= maxFailures;
    }

    public void recordFailure(String key) {
        if (entries.size() >= PURGE_THRESHOLD) {
            entries.values().removeIf(this::isExpired);
        }
        // Garde-fou mémoire : si quelqu'un génère des dizaines de milliers de clés différentes,
        // on cesse d'en mémoriser de nouvelles plutôt que de saturer le serveur.
        if (entries.size() >= HARD_CAP && !entries.containsKey(key)) {
            return;
        }
        Instant now = clock.instant();
        entries.compute(key, (k, entry) ->
                (entry == null || isExpired(entry))
                        ? new Entry(1, now)
                        : new Entry(entry.failures() + 1, entry.windowStart()));
    }

    /** Appelé après une connexion réussie : on repart à zéro. */
    public void reset(String key) {
        entries.remove(key);
    }

    private boolean isExpired(Entry entry) {
        return clock.instant().isAfter(entry.windowStart().plus(window));
    }
}

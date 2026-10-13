package fr.antrecapricorne.users.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Vérifie que chaque requête vient bien de la webapp, grâce à un secret partagé.
 *
 * <p>Pourquoi ? Sur le réseau Docker interne, n'importe quel conteneur pourrait appeler
 * {@code users-service}. Si un jour un autre service est compromis, il ne pourra pas créer
 * de comptes ni tester des mots de passe sans connaître ce secret. C'est une « défense en
 * profondeur » : une couche de plus, au cas où une autre cède.</p>
 *
 * <p>Principes appliqués :</p>
 * <ul>
 *   <li><b>Tout est refusé par défaut.</b> Seule la sonde de santé ({@code /actuator/health}),
 *       utilisée par Docker, est exemptée. Une nouvelle route ajoutée demain sera protégée sans
 *       qu'on ait à y penser.</li>
 *   <li><b>Comparaison à temps constant</b> ({@link MessageDigest#isEqual}) : un {@code equals}
 *       classique s'arrête à la première lettre fausse, et ce léger écart de durée permettrait
 *       de deviner le secret lettre par lettre.</li>
 *   <li><b>Démarrage refusé</b> si le secret fait moins de 32 caractères : un secret faible
 *       est pire que pas de secret, car il donne une fausse impression de sécurité.</li>
 *   <li>Le secret n'apparaît jamais dans les logs.</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InternalSecretFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Secret";
    static final int MIN_SECRET_LENGTH = 32;

    private static final Logger log = LoggerFactory.getLogger(InternalSecretFilter.class);

    private final byte[] expected;

    public InternalSecretFilter(@Value("${internal.secret}") String secret) {
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "INTERNAL_SECRET doit faire au moins " + MIN_SECRET_LENGTH + " caractères. "
                            + "Génère-en un nouveau dans ton fichier .env.");
        }
        this.expected = secret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/actuator/health") || path.startsWith("/actuator/health/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        boolean valid = provided != null
                && MessageDigest.isEqual(expected, provided.getBytes(StandardCharsets.UTF_8));

        if (!valid) {
            // On journalise le chemin, jamais l'en-tête reçu.
            log.warn("Appel interne refusé (secret absent ou incorrect) : {} {}", request.getMethod(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        chain.doFilter(request, response);
    }
}

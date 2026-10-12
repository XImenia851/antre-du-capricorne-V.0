package fr.antrecapricorne.webapp.security;

import fr.antrecapricorne.webapp.users.UsersClient;
import fr.antrecapricorne.webapp.users.UsersServiceUnavailableException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Vérifie une connexion en demandant à users-service.
 *
 * <p>Spring Security s'occupe de la mécanique (le formulaire, la session, la protection contre la
 * fixation de session). Notre rôle ici est seulement de répondre à une question : « ces identifiants
 * sont-ils bons ? ». Le mot de passe n'est ni haché ni comparé dans la webapp : c'est le travail
 * de users-service.</p>
 */
@Component
public class UsersAuthenticationProvider implements AuthenticationProvider {

    private final UsersClient users;
    private final LoginAttemptService attempts;

    public UsersAuthenticationProvider(UsersClient users, LoginAttemptService attempts) {
        this.users = users;
        this.attempts = attempts;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        String password = authentication.getCredentials() == null ? "" : authentication.getCredentials().toString();
        String key = attemptKey(authentication, email);

        if (attempts.isBlocked(key)) {
            throw new LockedException("Trop de tentatives");
        }

        Optional<AccountPrincipal> account;
        try {
            account = users.verify(email, password);
        } catch (UsersServiceUnavailableException e) {
            // Ce n'est pas la faute de l'utilisateur : on ne compte pas l'échec.
            throw new AuthenticationServiceException("Service des comptes indisponible", e);
        }

        if (account.isEmpty()) {
            attempts.recordFailure(key);
            // Un seul message pour tous les cas (compte inconnu, mauvais mot de passe, compte désactivé).
            throw new BadCredentialsException("Identifiants invalides");
        }

        attempts.reset(key);
        AccountPrincipal principal = account.get();
        return UsernamePasswordAuthenticationToken.authenticated(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + principal.role())));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    /** Clé « adresse IP + e-mail en minuscules » (voir LoginAttemptService). */
    static String attemptKey(Authentication authentication, String email) {
        String ip = "inconnue";
        if (authentication.getDetails() instanceof WebAuthenticationDetails details) {
            ip = details.getRemoteAddress();
        }
        return ip + "|" + email.trim().toLowerCase();
    }
}

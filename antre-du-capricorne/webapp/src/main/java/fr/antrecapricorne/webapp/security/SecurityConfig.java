package fr.antrecapricorne.webapp.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import java.time.Clock;
import java.time.Duration;

/**
 * Configuration de la sécurité de la webapp, écrite à la main pour être comprise ligne par ligne.
 *
 * <p>Principe directeur : TOUT est interdit par défaut. Une page n'est publique que si elle est
 * listée explicitement ci-dessous. Si on oublie de lister une nouvelle page, elle exigera une
 * connexion : l'oubli est sans danger.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** 5 échecs maximum par tranche de 15 minutes, pour une même IP + un même e-mail. */
    @Bean
    public LoginAttemptService loginAttemptService() {
        return new LoginAttemptService(5, Duration.ofMinutes(15), Clock.systemUTC());
    }

    /**
     * Le gestionnaire d'authentification, construit à la main avec UN SEUL fournisseur : le nôtre.
     *
     * <p>Pourquoi explicite ? Spring Security sait aussi détecter tout seul un fournisseur déclaré
     * comme bean ; si on le lui donne en plus à la main, il l'enregistre deux fois et le consulte
     * deux fois à chaque connexion (deux appels à users-service, deux échecs comptés). C'est un bug
     * que les tests ont révélé : ici, il ne peut plus se produire.</p>
     */
    @Bean
    public AuthenticationManager authenticationManager(UsersAuthenticationProvider authenticationProvider) {
        return new ProviderManager(authenticationProvider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationManager authenticationManager) throws Exception {
        http
                .authenticationManager(authenticationManager)

                // ---- Qui a le droit d'aller où ? (les règles sont lues de haut en bas) ----
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico", "/error").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/", "/connexion", "/inscription").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())

                // ---- Connexion ----
                .formLogin(form -> form
                        .loginPage("/connexion")
                        .loginProcessingUrl("/connexion")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/compte")
                        .failureHandler(loginFailureHandler()))

                // ---- Déconnexion : en POST uniquement (protégée par le jeton CSRF) ----
                .logout(logout -> logout
                        .logoutUrl("/deconnexion")
                        .logoutSuccessUrl("/?deconnecte")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))

                // ---- En-têtes de sécurité envoyés au navigateur ----
                // CSRF est activé par défaut : chaque formulaire POST doit porter un jeton secret.
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; "
                                        + "script-src 'self'; "
                                        + "style-src 'self'; "
                                        + "img-src 'self' data:; "
                                        + "object-src 'none'; "
                                        + "base-uri 'self'; "
                                        + "form-action 'self'; "
                                        + "frame-ancestors 'none'"))
                        .referrerPolicy(referrer -> referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)));

        return http.build();
    }

    /** Choisit le message affiché selon la cause de l'échec (sans jamais dire QUOI était faux). */
    private AuthenticationFailureHandler loginFailureHandler() {
        return (request, response, exception) -> {
            String target;
            if (exception instanceof LockedException) {
                target = "/connexion?limite";
            } else if (exception instanceof AuthenticationServiceException) {
                target = "/connexion?indisponible";
            } else {
                target = "/connexion?erreur";
            }
            response.sendRedirect(request.getContextPath() + target);
        };
    }
}

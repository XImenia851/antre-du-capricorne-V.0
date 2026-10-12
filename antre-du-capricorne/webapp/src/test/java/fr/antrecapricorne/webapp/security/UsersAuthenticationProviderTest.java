package fr.antrecapricorne.webapp.security;

import fr.antrecapricorne.webapp.users.UsersClient;
import fr.antrecapricorne.webapp.users.UsersServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** users-service est simulé (mock) : on teste uniquement la logique de la webapp. */
@ExtendWith(MockitoExtension.class)
class UsersAuthenticationProviderTest {

    private static final String EMAIL = "luna@example.com";

    @Mock
    private UsersClient users;

    private UsersAuthenticationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new UsersAuthenticationProvider(users,
                new LoginAttemptService(5, Duration.ofMinutes(15), Clock.systemUTC()));
    }

    private Authentication attempt(String password) {
        return UsernamePasswordAuthenticationToken.unauthenticated(EMAIL, password);
    }

    @Test
    void connexion_reussie_donne_le_role_et_garde_l_identite() {
        AccountPrincipal luna = new AccountPrincipal(UUID.randomUUID(), "Luna", "USER");
        when(users.verify(EMAIL, "bon-mot-de-passe")).thenReturn(Optional.of(luna));

        Authentication result = provider.authenticate(attempt("bon-mot-de-passe"));

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getPrincipal()).isEqualTo(luna);
        assertThat(result.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_USER");
        assertThat(result.getCredentials()).isNull();   // le mot de passe n'est pas conservé
    }

    @Test
    void identifiants_refuses_donnent_une_erreur_generique() {
        when(users.verify(EMAIL, "mauvais")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> provider.authenticate(attempt("mauvais")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void apres_5_echecs_meme_le_bon_mot_de_passe_est_refuse() {
        when(users.verify(EMAIL, "mauvais")).thenReturn(Optional.empty());
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> provider.authenticate(attempt("mauvais")))
                    .isInstanceOf(BadCredentialsException.class);
        }

        assertThatThrownBy(() -> provider.authenticate(attempt("bon-mot-de-passe")))
                .isInstanceOf(LockedException.class);
        // users-service n'est même plus interrogé pour le 6e essai
        verify(users, never()).verify(EMAIL, "bon-mot-de-passe");
    }

    @Test
    void panne_de_users_service_n_est_pas_comptee_comme_un_echec() {
        when(users.verify(EMAIL, "mot-de-passe")).thenThrow(new UsersServiceUnavailableException(new RuntimeException()));
        for (int i = 0; i < 6; i++) {
            assertThatThrownBy(() -> provider.authenticate(attempt("mot-de-passe")))
                    .isInstanceOf(AuthenticationServiceException.class);
        }
    }

    @Test
    void ne_traite_que_les_connexions_par_formulaire() {
        assertThat(provider.supports(UsernamePasswordAuthenticationToken.class)).isTrue();
        assertThat(provider.supports(String.class)).isFalse();
    }
}

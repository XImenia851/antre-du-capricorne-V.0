package fr.antrecapricorne.users.account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires : pas de base de données, le repository est simulé (mock).
 * On utilise un vrai BCrypt, mais avec un coût de 4 (au lieu de 12) pour que les tests restent rapides.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String PASSWORD = "un-mot-de-passe-solide";

    @Mock
    private UserRepository repository;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(repository, encoder);
    }

    // ---------- Inscription ----------

    @Test
    void register_stocke_un_hash_et_jamais_le_mot_de_passe_en_clair() {
        when(repository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = service.register("Luna_88", "  Luna@Example.COM ", PASSWORD);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository).saveAndFlush(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(saved.getPasswordHash()).startsWith("$2");
        assertThat(encoder.matches(PASSWORD, saved.getPasswordHash())).isTrue();
        assertThat(saved.getEmail()).isEqualTo("luna@example.com");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.isEnabled()).isTrue();
        assertThat(created.getPseudo()).isEqualTo("Luna_88");
    }

    @Test
    void register_refuse_un_pseudo_deja_pris() {
        when(repository.existsByPseudo("Luna")).thenReturn(true);

        assertThatThrownBy(() -> service.register("Luna", "luna@example.com", PASSWORD))
                .isInstanceOf(DuplicateUserException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void register_refuse_un_email_deja_pris() {
        when(repository.existsByPseudo("Luna")).thenReturn(false);
        when(repository.existsByEmail("luna@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register("Luna", "LUNA@example.com", PASSWORD))
                .isInstanceOf(DuplicateUserException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void register_traduit_la_violation_d_unicite_en_doublon() {
        // Cas de course : deux inscriptions passent le contrôle en même temps, la base tranche.
        when(repository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("ux_users_email_lower"));

        assertThatThrownBy(() -> service.register("Luna", "luna@example.com", PASSWORD))
                .isInstanceOf(DuplicateUserException.class);
    }

    @Test
    void register_refuse_un_mot_de_passe_de_plus_de_72_octets() {
        // 40 caractères « é » = 80 octets en UTF-8, alors que 40 < 72 caractères.
        String tropLong = "é".repeat(40);

        assertThatThrownBy(() -> service.register("Luna", "luna@example.com", tropLong))
                .isInstanceOf(InvalidPasswordException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    // ---------- Vérification des identifiants ----------

    private User activeUser() {
        return new User(UUID.randomUUID(), "Luna", "luna@example.com", encoder.encode(PASSWORD), Role.USER);
    }

    @Test
    void verify_accepte_les_bons_identifiants() {
        User user = activeUser();
        when(repository.findByEmail("luna@example.com")).thenReturn(Optional.of(user));

        assertThat(service.verify("luna@example.com", PASSWORD)).contains(user);
    }

    @Test
    void verify_refuse_un_mauvais_mot_de_passe() {
        when(repository.findByEmail("luna@example.com")).thenReturn(Optional.of(activeUser()));

        assertThat(service.verify("luna@example.com", "mauvais-mot-de-passe")).isEmpty();
    }

    @Test
    void verify_refuse_un_email_inconnu() {
        when(repository.findByEmail("inconnu@example.com")).thenReturn(Optional.empty());

        assertThat(service.verify("inconnu@example.com", PASSWORD)).isEmpty();
    }

    @Test
    void verify_refuse_un_compte_desactive_meme_avec_le_bon_mot_de_passe() {
        User user = activeUser();
        user.disable();
        when(repository.findByEmail("luna@example.com")).thenReturn(Optional.of(user));

        assertThat(service.verify("luna@example.com", PASSWORD)).isEmpty();
    }
}

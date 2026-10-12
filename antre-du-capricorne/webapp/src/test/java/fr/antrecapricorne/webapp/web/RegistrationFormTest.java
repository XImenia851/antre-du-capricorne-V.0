package fr.antrecapricorne.webapp.web;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrationFormTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void init() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    private RegistrationForm form(String pseudo, String email, String password) {
        RegistrationForm f = new RegistrationForm();
        f.setPseudo(pseudo);
        f.setEmail(email);
        f.setPassword(password);
        f.setPasswordConfirm(password);
        return f;
    }

    @Test
    void formulaire_valide() {
        assertThat(validator.validate(form("Luna_88", "luna@example.com", "un-mot-de-passe-solide"))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "Léa", "lu na", "<script>", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    void pseudos_invalides(String pseudo) {
        assertThat(validator.validate(form(pseudo, "luna@example.com", "un-mot-de-passe-solide"))).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"luna", "luna@", "@example.com", "luna@example", "lu na@example.com"})
    void emails_invalides(String email) {
        assertThat(validator.validate(form("Luna", email, "un-mot-de-passe-solide"))).isNotEmpty();
    }

    @Test
    void mot_de_passe_borne_a_12_et_72_caracteres() {
        assertThat(validator.validate(form("Luna", "luna@example.com", "a".repeat(11)))).isNotEmpty();
        assertThat(validator.validate(form("Luna", "luna@example.com", "a".repeat(12)))).isEmpty();
        assertThat(validator.validate(form("Luna", "luna@example.com", "a".repeat(72)))).isEmpty();
        assertThat(validator.validate(form("Luna", "luna@example.com", "a".repeat(73)))).isNotEmpty();
    }

    @Test
    void toString_ne_montre_jamais_le_mot_de_passe() {
        assertThat(form("Luna", "luna@example.com", "un-mot-de-passe-solide").toString())
                .doesNotContain("un-mot-de-passe-solide");
    }
}

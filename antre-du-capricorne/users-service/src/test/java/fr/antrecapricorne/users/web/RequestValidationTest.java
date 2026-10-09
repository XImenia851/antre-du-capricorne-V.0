package fr.antrecapricorne.users.web;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Teste les règles (REGEX, tailles) sans démarrer Spring : un simple Validator suffit. */
class RequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    private static final String OK_EMAIL = "luna@example.com";
    private static final String OK_PASSWORD = "un-mot-de-passe-solide";

    @BeforeAll
    static void init() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    private boolean pseudoValide(String pseudo) {
        return validator.validate(new RegisterRequest(pseudo, OK_EMAIL, OK_PASSWORD)).isEmpty();
    }

    private boolean emailValide(String email) {
        return validator.validate(new RegisterRequest("Luna", email, OK_PASSWORD)).isEmpty();
    }

    private boolean passwordValide(String password) {
        return validator.validate(new RegisterRequest("Luna", OK_EMAIL, password)).isEmpty();
    }

    // ---------- Pseudo ----------

    @ParameterizedTest
    @ValueSource(strings = {"abc", "Luna_88", "moon-child", "A1_-b2", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    void pseudos_valides(String pseudo) {
        assertThat(pseudoValide(pseudo)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "Léa", "lu na", "<script>", "luna!", "", "luna\n"})
    void pseudos_invalides(String pseudo) {
        assertThat(pseudoValide(pseudo)).isFalse();
    }

    @Test
    void pseudo_obligatoire() {
        assertThat(pseudoValide(null)).isFalse();
    }

    // ---------- E-mail ----------

    @ParameterizedTest
    @ValueSource(strings = {"luna@example.com", "a.b+tag@sous.domaine.fr", "x_y%z@mail-server.org"})
    void emails_valides(String email) {
        assertThat(emailValide(email)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"luna", "luna@", "@example.com", "luna@example", "luna@example.c", "lu na@example.com",
            "luna@@example.com", "luna@exa mple.com", ""})
    void emails_invalides(String email) {
        assertThat(emailValide(email)).isFalse();
    }

    @Test
    void email_de_plus_de_254_caracteres_refuse() {
        String trop = "a".repeat(250) + "@example.com";
        assertThat(emailValide(trop)).isFalse();
    }

    // ---------- Mot de passe ----------

    @Test
    void mot_de_passe_borne_a_12_et_72_caracteres() {
        assertThat(passwordValide("a".repeat(11))).isFalse();
        assertThat(passwordValide("a".repeat(12))).isTrue();
        assertThat(passwordValide("a".repeat(72))).isTrue();
        assertThat(passwordValide("a".repeat(73))).isFalse();
    }

    @Test
    void verify_request_limite_la_taille_mais_pas_le_format() {
        assertThat(validator.validate(new VerifyRequest("n'importe quoi", "court"))).isEmpty();
        assertThat(validator.validate(new VerifyRequest(OK_EMAIL, "a".repeat(73)))).isNotEmpty();
    }

    // ---------- Pas de fuite du mot de passe ----------

    @Test
    void toString_ne_montre_jamais_le_mot_de_passe() {
        assertThat(new RegisterRequest("Luna", OK_EMAIL, OK_PASSWORD).toString()).doesNotContain(OK_PASSWORD);
        assertThat(new VerifyRequest(OK_EMAIL, OK_PASSWORD).toString()).doesNotContain(OK_PASSWORD);
    }
}

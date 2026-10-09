package fr.antrecapricorne.users.web;

import fr.antrecapricorne.users.account.ValidationRules;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Corps JSON de l'inscription. Aucun champ « role » : on ne peut pas se déclarer admin. */
public record RegisterRequest(
        @NotNull(message = "Le pseudo est obligatoire.")
        @Pattern(regexp = ValidationRules.PSEUDO_REGEX, message = ValidationRules.PSEUDO_MESSAGE)
        String pseudo,

        @NotNull(message = "L'adresse e-mail est obligatoire.")
        @Size(max = ValidationRules.EMAIL_MAX_LENGTH, message = ValidationRules.EMAIL_LENGTH_MESSAGE)
        @Pattern(regexp = ValidationRules.EMAIL_REGEX, message = ValidationRules.EMAIL_MESSAGE)
        String email,

        @NotNull(message = "Le mot de passe est obligatoire.")
        @Size(min = ValidationRules.PASSWORD_MIN_LENGTH, max = ValidationRules.PASSWORD_MAX_LENGTH,
                message = ValidationRules.PASSWORD_MESSAGE)
        String password) {

    /** Ne jamais afficher le mot de passe, même par accident dans un log. */
    @Override
    public String toString() {
        return "RegisterRequest[pseudo=" + pseudo + ", email=" + email + ", password=***]";
    }
}

package fr.antrecapricorne.users.web;

import fr.antrecapricorne.users.account.ValidationRules;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corps JSON de la vérification des identifiants (connexion).
 * Pas de REGEX ici : on ne donne aucun indice sur le format attendu à quelqu'un qui devine.
 * La longueur est bornée pour éviter d'envoyer des textes énormes.
 */
public record VerifyRequest(
        @NotNull(message = "L'adresse e-mail est obligatoire.")
        @Size(max = ValidationRules.EMAIL_MAX_LENGTH, message = ValidationRules.EMAIL_LENGTH_MESSAGE)
        String email,

        @NotNull(message = "Le mot de passe est obligatoire.")
        @Size(max = ValidationRules.PASSWORD_MAX_LENGTH, message = ValidationRules.PASSWORD_MESSAGE)
        String password) {

    @Override
    public String toString() {
        return "VerifyRequest[email=" + email + ", password=***]";
    }
}

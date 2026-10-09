package fr.antrecapricorne.users.account;

/** Le mot de passe dépasse les 72 octets que BCrypt sait lire. */
public class InvalidPasswordException extends RuntimeException {

    public InvalidPasswordException() {
        super("Mot de passe trop long en octets");
    }
}

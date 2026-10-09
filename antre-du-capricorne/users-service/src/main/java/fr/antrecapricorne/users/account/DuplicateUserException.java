package fr.antrecapricorne.users.account;

/** Le pseudo ou l'adresse e-mail est déjà pris. */
public class DuplicateUserException extends RuntimeException {

    public DuplicateUserException() {
        super("Pseudo ou e-mail déjà utilisé");
    }
}

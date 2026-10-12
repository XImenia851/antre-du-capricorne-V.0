package fr.antrecapricorne.webapp.users;

/** users-service ne répond pas (arrêté, trop lent, erreur interne). */
public class UsersServiceUnavailableException extends RuntimeException {

    public UsersServiceUnavailableException(Throwable cause) {
        super("users-service indisponible", cause);
    }
}

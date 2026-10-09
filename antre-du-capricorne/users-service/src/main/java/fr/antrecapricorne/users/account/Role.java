package fr.antrecapricorne.users.account;

/**
 * Rôles possibles. Ils correspondent à la contrainte {@code ck_users_role} de la base.
 * L'inscription publique ne crée jamais autre chose que USER.
 */
public enum Role {
    USER,
    MODERATOR,
    ADMIN
}

package fr.antrecapricorne.users.web;

import fr.antrecapricorne.users.account.Role;
import fr.antrecapricorne.users.account.User;

import java.util.UUID;

/** Ce que l'API renvoie : jamais le hash du mot de passe, ni l'e-mail. */
public record UserResponse(UUID id, String pseudo, Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getPseudo(), user.getRole());
    }
}

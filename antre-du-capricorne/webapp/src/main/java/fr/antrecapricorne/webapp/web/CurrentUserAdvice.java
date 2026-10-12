package fr.antrecapricorne.webapp.web;

import fr.antrecapricorne.webapp.security.AccountPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Met l'utilisateur connecté à la disposition de TOUS les templates sous le nom « currentUser »
 * (valeur null si personne n'est connecté). Sert à l'en-tête : « Connexion » ou « Mon compte ».
 */
@ControllerAdvice
public class CurrentUserAdvice {

    @ModelAttribute("currentUser")
    public AccountPrincipal currentUser(@AuthenticationPrincipal AccountPrincipal principal) {
        return principal;
    }
}

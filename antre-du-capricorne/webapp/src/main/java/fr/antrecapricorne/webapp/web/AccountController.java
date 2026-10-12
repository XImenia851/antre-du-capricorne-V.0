package fr.antrecapricorne.webapp.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Espace personnel. Réservé aux personnes connectées (règle « anyRequest().authenticated() »).
 *
 * <p>Le pseudo affiché vient de la session (voir CurrentUserAdvice), jamais de l'URL.
 * Plus tard : la liste des cartes favorites de CET utilisateur, retrouvée grâce à son identifiant
 * de session et à aucun autre.</p>
 */
@Controller
public class AccountController {

    @GetMapping("/compte")
    public String account() {
        return "compte";
    }
}

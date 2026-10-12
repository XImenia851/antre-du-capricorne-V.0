package fr.antrecapricorne.webapp.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Espace d'administration. La règle d'accès est dans SecurityConfig ({@code /admin/**} exige le
 * rôle ADMIN) : un simple utilisateur reçoit une erreur 403, un visiteur est renvoyé vers la connexion.
 * Pour l'instant, c'est une page vide : la gestion des cartes arrive en phase 2.
 */
@Controller
public class AdminController {

    @GetMapping("/admin")
    public String admin() {
        return "admin/index";
    }
}

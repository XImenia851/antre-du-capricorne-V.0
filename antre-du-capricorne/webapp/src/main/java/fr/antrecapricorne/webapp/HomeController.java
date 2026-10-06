package fr.antrecapricorne.webapp;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Page d'accueil provisoire, pour vérifier que le service démarre.
 * Elle sera remplacée par la vraie couverture du grimoire (phase 2).
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }
}

package fr.antrecapricorne.webapp.web;

import fr.antrecapricorne.webapp.users.RegistrationResult;
import fr.antrecapricorne.webapp.users.UsersClient;
import fr.antrecapricorne.webapp.users.UsersServiceUnavailableException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Objects;
import java.util.Set;

/**
 * Pages d'inscription et de connexion.
 * Le traitement de la connexion elle-même (POST /connexion) est fait par Spring Security.
 */
@Controller
public class AuthController {

    /** Seuls ces champs peuvent recevoir une erreur venue de users-service (liste blanche). */
    private static final Set<String> FORM_FIELDS = Set.of("pseudo", "email", "password");

    private final UsersClient users;

    public AuthController(UsersClient users) {
        this.users = users;
    }

    @GetMapping("/connexion")
    public String loginPage() {
        return "connexion";
    }

    @GetMapping("/inscription")
    public String registrationPage(Model model) {
        model.addAttribute("form", new RegistrationForm());
        return "inscription";
    }

    @PostMapping("/inscription")
    public String register(@Valid @ModelAttribute("form") RegistrationForm form, BindingResult result) {
        if (!result.hasFieldErrors("password") && !result.hasFieldErrors("passwordConfirm")
                && !Objects.equals(form.getPassword(), form.getPasswordConfirm())) {
            result.rejectValue("passwordConfirm", "mismatch", "Les deux mots de passe ne correspondent pas.");
        }
        if (result.hasErrors()) {
            return "inscription";
        }

        try {
            RegistrationResult outcome = users.register(form.getPseudo(), form.getEmail(), form.getPassword());
            switch (outcome.status()) {
                case CREATED -> {
                    return "redirect:/connexion?inscrit";
                }
                case DUPLICATE -> result.reject("duplicate",
                        "Ce pseudo ou cette adresse e-mail ne peut pas être utilisé.");
                case INVALID -> {
                    outcome.fieldErrors().forEach((field, message) -> {
                        if (FORM_FIELDS.contains(field)) {
                            result.rejectValue(field, "service", message);
                        }
                    });
                    if (!result.hasErrors()) {
                        result.reject("invalid", "Certaines informations sont invalides.");
                    }
                }
            }
        } catch (UsersServiceUnavailableException e) {
            result.reject("unavailable", "Le service des comptes est momentanément indisponible. Réessaie dans un instant.");
        }
        return "inscription";
    }
}

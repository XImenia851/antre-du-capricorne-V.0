package fr.antrecapricorne.webapp.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Données du formulaire d'inscription.
 *
 * <p>Ces règles sont VOLONTAIREMENT les mêmes que dans users-service : la webapp valide en premier
 * pour répondre vite et afficher de belles erreurs, users-service valide en dernier car c'est lui
 * qui protège la base. Chaque service se défend seul : on ne fait jamais confiance à l'appelant,
 * même quand c'est « notre » webapp. (Les services ne partagent pas de code : si une règle change,
 * elle se change aux deux endroits.)</p>
 *
 * <p>Pas de champ « role » : impossible de se déclarer administrateur depuis le formulaire.</p>
 */
public class RegistrationForm {

    // Mêmes REGEX que users-service (expliquées caractère par caractère dans ValidationRules).
    static final String PSEUDO_REGEX = "^[A-Za-z0-9_-]{3,30}$";
    static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$";

    @NotBlank(message = "Le pseudo est obligatoire.")
    @Pattern(regexp = PSEUDO_REGEX,
            message = "Le pseudo doit faire de 3 à 30 caractères : lettres sans accent, chiffres, _ ou -.")
    private String pseudo;

    @NotBlank(message = "L'adresse e-mail est obligatoire.")
    @Size(max = 254, message = "L'adresse e-mail est trop longue (254 caractères maximum).")
    @Pattern(regexp = EMAIL_REGEX, message = "L'adresse e-mail n'est pas valide.")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire.")
    @Size(min = 12, max = 72, message = "Le mot de passe doit faire entre 12 et 72 caractères.")
    private String password;

    private String passwordConfirm;

    public String getPseudo() {
        return pseudo;
    }

    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordConfirm() {
        return passwordConfirm;
    }

    public void setPasswordConfirm(String passwordConfirm) {
        this.passwordConfirm = passwordConfirm;
    }

    /** Ne jamais afficher les mots de passe, même par accident dans un log. */
    @Override
    public String toString() {
        return "RegistrationForm[pseudo=" + pseudo + ", email=" + email + ", password=***]";
    }
}

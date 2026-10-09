package fr.antrecapricorne.users.account;

import java.nio.charset.StandardCharsets;

/**
 * Règles de validation des comptes, regroupées en un seul endroit.
 *
 * <p>Principe de sécurité : la validation se fait TOUJOURS côté serveur. Les contrôles d'un
 * formulaire HTML (ou de JavaScript) se contournent en deux secondes : ils servent au confort
 * de l'utilisateur, jamais à la protection.</p>
 *
 * <h2>Lire un REGEX, morceau par morceau</h2>
 *
 * <p><b>Pseudo</b> : {@code ^[A-Za-z0-9_-]{3,30}$}</p>

 * <p><b>Mot de passe</b> : 12 à 72 caractères, et 72 octets maximum une fois encodé en UTF-8.
 * BCrypt ne lit que les 72 premiers octets : au-delà, deux mots de passe différents donneraient
 * le même hash. Un « é » compte pour 2 octets, d'où le contrôle en octets en plus du contrôle
 * en caractères.</p>
 */
public final class ValidationRules {

    private ValidationRules() {
    }

    // ---- Pseudo ----
    public static final String PSEUDO_REGEX = "^[A-Za-z0-9_-]{3,30}$";
    public static final String PSEUDO_MESSAGE =
            "Le pseudo doit faire de 3 à 30 caractères : lettres sans accent, chiffres, _ ou -.";

    // ---- E-mail ----
    public static final int EMAIL_MAX_LENGTH = 254;
    public static final String EMAIL_REGEX =
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$";
    public static final String EMAIL_MESSAGE = "L'adresse e-mail n'est pas valide.";
    public static final String EMAIL_LENGTH_MESSAGE = "L'adresse e-mail est trop longue (254 caractères maximum).";

    // ---- Mot de passe ----
    public static final int PASSWORD_MIN_LENGTH = 12;
    public static final int PASSWORD_MAX_LENGTH = 72;
    public static final int BCRYPT_MAX_BYTES = 72;
    public static final String PASSWORD_MESSAGE = "Le mot de passe doit faire entre 12 et 72 caractères.";
    public static final String PASSWORD_BYTES_MESSAGE =
            "Le mot de passe est trop long : 72 octets maximum (un caractère accentué en compte 2).";

    /** Vrai si le mot de passe tient dans les 72 octets que BCrypt sait lire. */
    public static boolean fitsBcrypt(String rawPassword) {
        return rawPassword.getBytes(StandardCharsets.UTF_8).length <= BCRYPT_MAX_BYTES;
    }
}

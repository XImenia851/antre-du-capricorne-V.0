package fr.antrecapricorne.users.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    /**
     * Hachage des mots de passe avec BCrypt.
     *
     * <p>Le « coût » 12 signifie 2^12 tours de calcul : environ un quart de seconde par hash.
     * C'est imperceptible pour un utilisateur qui se connecte, mais cela rend une attaque par
     * essais successifs (des millions de mots de passe testés) extrêmement lente. Chaque hash
     * contient son propre « sel » aléatoire : deux comptes avec le même mot de passe ont deux
     * hashs différents.</p>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}

package fr.antrecapricorne.users;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Service des comptes utilisateurs. Il n'est jamais joignable depuis Internet :
 * seul le webapp l'appelle, sur le réseau Docker privé.
 */
@SpringBootApplication
public class UsersServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UsersServiceApplication.class, args);
    }
}

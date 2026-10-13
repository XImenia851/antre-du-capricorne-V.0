package fr.antrecapricorne.webapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Vérifie que le contexte Spring démarre. Des tests plus précis
 * (accès, validation des formulaires) arrivent avec Spring Security.
 */
// Le secret est fourni ici pour le test (valeur factice de plus de 32 caractères, sans aucune valeur réelle).
@SpringBootTest(properties = "internal.secret=secret-factice-uniquement-pour-les-tests-1234")
class WebappApplicationTests {

    @Test
    void contextLoads() {
    }
}

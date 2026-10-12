package fr.antrecapricorne.webapp.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    /** Horloge qu'on peut avancer à la main : pas besoin d'attendre 15 vraies minutes. */
    static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-12T08:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private MutableClock clock;
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock();
        service = new LoginAttemptService(5, Duration.ofMinutes(15), clock);
    }

    @Test
    void personne_n_est_bloque_au_depart() {
        assertThat(service.isBlocked("1.2.3.4|luna@example.com")).isFalse();
    }

    @Test
    void bloque_apres_5_echecs_mais_pas_avant() {
        String key = "1.2.3.4|luna@example.com";
        for (int i = 0; i < 4; i++) {
            service.recordFailure(key);
        }
        assertThat(service.isBlocked(key)).isFalse();

        service.recordFailure(key);
        assertThat(service.isBlocked(key)).isTrue();
    }

    @Test
    void le_blocage_est_propre_a_chaque_cle() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure("1.2.3.4|luna@example.com");
        }
        assertThat(service.isBlocked("1.2.3.4|luna@example.com")).isTrue();
        assertThat(service.isBlocked("9.9.9.9|luna@example.com")).isFalse();
        assertThat(service.isBlocked("1.2.3.4|autre@example.com")).isFalse();
    }

    @Test
    void le_blocage_se_leve_apres_la_fenetre() {
        String key = "1.2.3.4|luna@example.com";
        for (int i = 0; i < 5; i++) {
            service.recordFailure(key);
        }
        clock.advance(Duration.ofMinutes(14));
        assertThat(service.isBlocked(key)).isTrue();

        clock.advance(Duration.ofMinutes(2));
        assertThat(service.isBlocked(key)).isFalse();
    }

    @Test
    void une_connexion_reussie_remet_le_compteur_a_zero() {
        String key = "1.2.3.4|luna@example.com";
        for (int i = 0; i < 4; i++) {
            service.recordFailure(key);
        }
        service.reset(key);
        service.recordFailure(key);

        assertThat(service.isBlocked(key)).isFalse();
    }
}

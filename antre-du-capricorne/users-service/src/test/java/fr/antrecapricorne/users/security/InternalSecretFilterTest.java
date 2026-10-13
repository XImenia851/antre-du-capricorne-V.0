package fr.antrecapricorne.users.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pas de serveur ni de base : Spring fournit de fausses requêtes/réponses pour tester un filtre. */
class InternalSecretFilterTest {

    private static final String SECRET = "un-secret-de-test-bien-assez-long-1234";

    private InternalSecretFilter filter;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new InternalSecretFilter(SECRET);
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    private MockHttpServletRequest request(String method, String path, String secretHeader) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        if (secretHeader != null) {
            request.addHeader(InternalSecretFilter.HEADER, secretHeader);
        }
        return request;
    }

    /** Vrai si la requête a franchi le filtre et atteint la suite (le contrôleur). */
    private boolean passed() {
        return chain.getRequest() != null;
    }

    @Test
    void sans_secret_la_requete_est_refusee() throws Exception {
        filter.doFilter(request("POST", "/internal/users", null), response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(passed()).isFalse();
    }

    @Test
    void avec_un_mauvais_secret_la_requete_est_refusee() throws Exception {
        filter.doFilter(request("POST", "/internal/users", "mauvais-secret"), response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(passed()).isFalse();
    }

    @Test
    void un_secret_presque_bon_est_refuse() throws Exception {
        filter.doFilter(request("POST", "/internal/users", SECRET + "x"), response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(passed()).isFalse();
    }

    @Test
    void avec_le_bon_secret_la_requete_passe() throws Exception {
        filter.doFilter(request("POST", "/internal/users/verify", SECRET), response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(passed()).isTrue();
    }

    @Test
    void la_sonde_de_sante_reste_accessible_sans_secret() throws Exception {
        filter.doFilter(request("GET", "/actuator/health", null), response, chain);

        assertThat(passed()).isTrue();
    }

    @Test
    void les_sondes_liveness_et_readiness_restent_accessibles() throws Exception {
        filter.doFilter(request("GET", "/actuator/health/liveness", null), response, chain);

        assertThat(passed()).isTrue();
    }

    @Test
    void tout_le_reste_est_refuse_par_defaut() throws Exception {
        // Même une adresse « tordue » ou inconnue n'échappe pas au contrôle.
        for (String path : new String[]{"//internal/users", "/internal/../internal/users", "/nimporte/quoi", "/actuator/env"}) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            MockFilterChain ch = new MockFilterChain();

            filter.doFilter(request("GET", path, null), res, ch);

            assertThat(res.getStatus()).as(path).isEqualTo(401);
            assertThat(ch.getRequest()).as(path).isNull();
        }
    }

    @Test
    void un_secret_trop_court_empeche_le_demarrage() {
        assertThatThrownBy(() -> new InternalSecretFilter("trop-court"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    @Test
    void un_secret_absent_empeche_le_demarrage() {
        assertThatThrownBy(() -> new InternalSecretFilter(null))
                .isInstanceOf(IllegalStateException.class);
    }
}

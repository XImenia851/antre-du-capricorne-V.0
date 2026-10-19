package fr.antrecapricorne.webapp;

import fr.antrecapricorne.webapp.security.AccountPrincipal;
import fr.antrecapricorne.webapp.users.RegistrationResult;
import fr.antrecapricorne.webapp.users.UsersClient;
import fr.antrecapricorne.webapp.users.UsersServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Teste les règles d'accès de la webapp avec la vraie configuration Spring Security.
 * users-service est simulé (il n'est pas nécessaire ici) : on teste uniquement ce que la webapp décide.
 *
 * <p>MockMvc envoie de fausses requêtes HTTP à l'application sans démarrer de serveur : c'est rapide.</p>
 */
@SpringBootTest
class SecurityAccessTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private UsersClient usersClient;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    /** Fabrique un utilisateur connecté avec le rôle voulu (USER ou ADMIN). */
    private static Authentication connecte(String role) {
        AccountPrincipal principal = new AccountPrincipal(UUID.randomUUID(), "Luna", role);
        return UsernamePasswordAuthenticationToken.authenticated(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    // ---------- Pages publiques ----------

    @Test
    void les_pages_publiques_sont_accessibles_sans_connexion() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/connexion")).andExpect(status().isOk());
        mockMvc.perform(get("/inscription")).andExpect(status().isOk());
        mockMvc.perform(get("/css/site.css")).andExpect(status().isOk());
    }

    @Test
    void la_sonde_de_sante_est_publique() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    // ---------- Pages protégées ----------

    @Test
    void visiteur_non_connecte_est_renvoye_vers_la_connexion() throws Exception {
        mockMvc.perform(get("/compte"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/connexion"));
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/connexion"));
    }

    @Test
    void une_page_inconnue_exige_aussi_une_connexion() throws Exception {
        // Tout est interdit par défaut : même une adresse qui n'existe pas ne révèle rien aux visiteurs.
        mockMvc.perform(get("/une-page-quelconque"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/connexion"));
    }

    @Test
    void utilisateur_connecte_voit_son_compte() throws Exception {
        mockMvc.perform(get("/compte").with(authentication(connecte("USER"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Luna")));
    }

    @Test
    void simple_utilisateur_n_entre_pas_dans_l_admin() throws Exception {
        mockMvc.perform(get("/admin").with(authentication(connecte("USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void administrateur_entre_dans_l_admin() throws Exception {
        mockMvc.perform(get("/admin").with(authentication(connecte("ADMIN"))))
                .andExpect(status().isOk());
    }

    // ---------- CSRF ----------

    @Test
    void formulaires_sans_jeton_csrf_sont_refuses() throws Exception {
        mockMvc.perform(post("/inscription")
                        .param("pseudo", "Luna").param("email", "luna@example.com")
                        .param("password", "un-mot-de-passe-solide").param("passwordConfirm", "un-mot-de-passe-solide"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/connexion").param("email", "luna@example.com").param("password", "x"))
                .andExpect(status().isForbidden());
        verify(usersClient, never()).register(any(), any(), any());
    }

    @Test
    void la_deconnexion_exige_un_post_avec_jeton() throws Exception {
        mockMvc.perform(post("/deconnexion").with(authentication(connecte("USER"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/deconnexion").with(authentication(connecte("USER"))).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?deconnecte"));
    }

    // ---------- En-têtes de sécurité ----------

    @Test
    void les_en_tetes_de_securite_sont_envoyes() throws Exception {
        mockMvc.perform(get("/connexion"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")))
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists("Referrer-Policy"));
    }

    // ---------- Inscription ----------

    @Test
    void inscription_valide_redirige_vers_la_connexion() throws Exception {
        when(usersClient.register("Luna_88", "luna88@example.com", "un-mot-de-passe-solide"))
                .thenReturn(RegistrationResult.created());

        mockMvc.perform(post("/inscription").with(csrf())
                        .param("pseudo", "Luna_88").param("email", "luna88@example.com")
                        .param("password", "un-mot-de-passe-solide").param("passwordConfirm", "un-mot-de-passe-solide"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/connexion?inscrit"));
    }

    @Test
    void inscription_invalide_reste_sur_le_formulaire_sans_appeler_users_service() throws Exception {
        mockMvc.perform(post("/inscription").with(csrf())
                        .param("pseudo", "Léa").param("email", "pas-un-email")
                        .param("password", "court").param("passwordConfirm", "autre"))
                .andExpect(status().isOk())
                .andExpect(view().name("inscription"))
                // Le mot de passe saisi n'est jamais renvoyé dans la page.
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("court\""))));
        verify(usersClient, never()).register(any(), any(), any());
    }

    @Test
    void deux_mots_de_passe_differents_sont_refuses() throws Exception {
        mockMvc.perform(post("/inscription").with(csrf())
                        .param("pseudo", "Luna").param("email", "luna@example.com")
                        .param("password", "un-mot-de-passe-solide").param("passwordConfirm", "un-autre-mot-de-passe"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ne correspondent pas")));
        verify(usersClient, never()).register(any(), any(), any());
    }

    @Test
    void pseudo_ou_email_deja_pris_donne_un_message_sans_preciser_lequel() throws Exception {
        when(usersClient.register(any(), any(), any())).thenReturn(RegistrationResult.duplicate());

        mockMvc.perform(post("/inscription").with(csrf())
                        .param("pseudo", "Luna").param("email", "luna@example.com")
                        .param("password", "un-mot-de-passe-solide").param("passwordConfirm", "un-mot-de-passe-solide"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ne peut pas être utilisé")));
    }

    @Test
    void panne_de_users_service_pendant_l_inscription_affiche_un_message() throws Exception {
        when(usersClient.register(any(), any(), any())).thenThrow(new UsersServiceUnavailableException(new RuntimeException()));

        mockMvc.perform(post("/inscription").with(csrf())
                        .param("pseudo", "Luna").param("email", "luna@example.com")
                        .param("password", "un-mot-de-passe-solide").param("passwordConfirm", "un-mot-de-passe-solide"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("momentanément indisponible")));
    }

    // ---------- Connexion ----------

    @Test
    void bons_identifiants_redirigent_vers_le_compte() throws Exception {
        when(usersClient.verify("ok@example.com", "bon-mot-de-passe"))
                .thenReturn(Optional.of(new AccountPrincipal(UUID.randomUUID(), "Luna", "USER")));

        mockMvc.perform(post("/connexion").with(csrf())
                        .param("email", "ok@example.com").param("password", "bon-mot-de-passe"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/compte"));
    }

    @Test
    void mauvais_identifiants_renvoient_une_erreur_generique() throws Exception {
        when(usersClient.verify("faux@example.com", "mauvais")).thenReturn(Optional.empty());

        mockMvc.perform(post("/connexion").with(csrf())
                        .param("email", "faux@example.com").param("password", "mauvais"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/connexion?erreur"));
    }

    @Test
    void apres_5_echecs_la_connexion_est_bloquee() throws Exception {
        when(usersClient.verify("bloque@example.com", "mauvais")).thenReturn(Optional.empty());
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/connexion").with(csrf())
                            .param("email", "bloque@example.com").param("password", "mauvais"))
                    .andExpect(redirectedUrl("/connexion?erreur"));
        }

        mockMvc.perform(post("/connexion").with(csrf())
                        .param("email", "bloque@example.com").param("password", "mauvais"))
                .andExpect(redirectedUrl("/connexion?limite"));
    }

    @Test
    void un_echec_de_connexion_n_interroge_users_service_qu_une_seule_fois() throws Exception {
        // Régression : le fournisseur d'authentification était consulté deux fois par tentative.
        when(usersClient.verify("une-fois@example.com", "mauvais")).thenReturn(Optional.empty());

        mockMvc.perform(post("/connexion").with(csrf())
                        .param("email", "une-fois@example.com").param("password", "mauvais"))
                .andExpect(redirectedUrl("/connexion?erreur"));

        verify(usersClient, times(1)).verify("une-fois@example.com", "mauvais");
    }

    @Test
    void panne_de_users_service_pendant_la_connexion_est_signalee() throws Exception {
        when(usersClient.verify("panne@example.com", "x"))
                .thenThrow(new UsersServiceUnavailableException(new RuntimeException()));

        mockMvc.perform(post("/connexion").with(csrf())
                        .param("email", "panne@example.com").param("password", "x"))
                .andExpect(redirectedUrl("/connexion?indisponible"));
    }
}

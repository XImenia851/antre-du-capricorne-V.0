package fr.antrecapricorne.users;

import fr.antrecapricorne.users.account.Role;
import fr.antrecapricorne.users.account.User;
import fr.antrecapricorne.users.account.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.net.http.HttpClient;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test d'intégration : la VRAIE application démarre, avec une VRAIE base PostgreSQL (dans un conteneur
 * Docker jetable). On appelle les vraies routes HTTP, comme le fera la webapp.
 *
 * <p>Ce que ça prouve que les tests unitaires ne prouvent pas : la migration Flyway crée bien la table,
 * Hibernate est d'accord avec elle, les index uniques protègent vraiment les doublons, le filtre de
 * secret est bien branché devant les routes, et le JSON circule comme prévu.</p>
 *
 * <p>Docker doit être allumé pour lancer ce test.</p>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "internal.secret=" + UsersApiIntegrationTest.SECRET)
class UsersApiIntegrationTest {

    static final String SECRET = "secret-de-test-uniquement-1234567890-abcdef";
    private static final String PASSWORD = "un-mot-de-passe-solide";

    // Conteneur unique pour toute la classe : démarré une fois, supprimé automatiquement à la fin.
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    /** Branche l'application sur la base jetable (remplace DB_URL, DB_USER, DB_PASSWORD). */
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private UserRepository repository;

    @Autowired
    private PasswordEncoder encoder;

    // ---------- Outils ----------

    private record Reply(int status, String body) {
    }

    private Reply post(String path, String json, String secret) {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .requestFactory(new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()))
                .build();
        RestClient.RequestBodySpec request = client.post().uri(path).contentType(MediaType.APPLICATION_JSON);
        if (secret != null) {
            request.header("X-Internal-Secret", secret);
        }
        return request.body(json).exchange((req, res) -> new Reply(res.getStatusCode().value(), res.bodyTo(String.class)));
    }

    private Reply get(String path) {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .requestFactory(new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()))
                .build();
        return client.get().uri(path).exchange((req, res) -> new Reply(res.getStatusCode().value(), res.bodyTo(String.class)));
    }

    /** Suffixe aléatoire : chaque test utilise ses propres pseudo et e-mail, sans se marcher dessus. */
    private static String unique() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private static String registerJson(String pseudo, String email, String password) {
        return "{\"pseudo\":\"" + pseudo + "\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private static String verifyJson(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    // ---------- Inscription ----------

    @Test
    void inscription_cree_le_compte_et_stocke_un_hash_en_base() {
        String id = unique();
        String email = "luna_" + id + "@example.com";

        Reply reply = post("/internal/users", registerJson("luna_" + id, email, PASSWORD), SECRET);

        assertThat(reply.status()).isEqualTo(201);
        // La réponse montre pseudo et rôle, jamais le mot de passe ni son hash.
        assertThat(reply.body()).contains("luna_" + id).contains("USER");
        assertThat(reply.body()).doesNotContain(PASSWORD).doesNotContain("$2").doesNotContain("password");

        User saved = repository.findByEmail(email).orElseThrow();
        assertThat(saved.getPasswordHash()).startsWith("$2").isNotEqualTo(PASSWORD);
        assertThat(encoder.matches(PASSWORD, saved.getPasswordHash())).isTrue();
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.getCreatedAt()).isNotNull();   // rempli par la base (DEFAULT now())
    }

    @Test
    void sans_le_secret_interne_rien_n_est_cree() {
        String id = unique();
        String email = "intrus_" + id + "@example.com";

        Reply reply = post("/internal/users", registerJson("intrus_" + id, email, PASSWORD), null);
        Reply wrong = post("/internal/users", registerJson("intrus_" + id, email, PASSWORD), "mauvais-secret");

        assertThat(reply.status()).isEqualTo(401);
        assertThat(wrong.status()).isEqualTo(401);
        assertThat(repository.findByEmail(email)).isEmpty();
    }

    @Test
    void doublon_d_email_refuse_meme_avec_des_majuscules() {
        String id = unique();
        post("/internal/users", registerJson("anna_" + id, "anna_" + id + "@example.com", PASSWORD), SECRET);

        Reply reply = post("/internal/users",
                registerJson("autre_" + id, "ANNA_" + id + "@EXAMPLE.com", PASSWORD), SECRET);

        assertThat(reply.status()).isEqualTo(409);
        assertThat(reply.body()).doesNotContain("email").doesNotContain("pseudo");   // message volontairement vague
    }

    @Test
    void doublon_de_pseudo_refuse_meme_avec_des_majuscules() {
        String id = unique();
        post("/internal/users", registerJson("Zoe_" + id, "zoe_" + id + "@example.com", PASSWORD), SECRET);

        Reply reply = post("/internal/users",
                registerJson("ZOE_" + id, "zoe2_" + id + "@example.com", PASSWORD), SECRET);

        assertThat(reply.status()).isEqualTo(409);
    }

    @Test
    void la_base_elle_meme_refuse_les_doublons() {
        // Même si le code de l'application avait un bug, les index uniques de la migration V1 protègent.
        String id = unique();
        repository.saveAndFlush(new User(UUID.randomUUID(), "Base_" + id, "base_" + id + "@example.com",
                encoder.encode(PASSWORD), Role.USER));

        User twin = new User(UUID.randomUUID(), "BASE_" + id, "autre_" + id + "@example.com",
                encoder.encode(PASSWORD), Role.USER);

        assertThatThrownBy(() -> repository.saveAndFlush(twin))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void donnees_invalides_donnent_400_avec_les_champs_en_cause() {
        Reply reply = post("/internal/users", registerJson("Léa", "pas-un-email", "court"), SECRET);

        assertThat(reply.status()).isEqualTo(400);
        assertThat(reply.body()).contains("pseudo").contains("email").contains("password");
        assertThat(reply.body()).doesNotContain("court");   // la valeur refusée n'est jamais renvoyée
    }

    @Test
    void mot_de_passe_de_plus_de_72_octets_refuse() {
        String id = unique();
        String tropLong = "é".repeat(40);   // 40 caractères mais 80 octets : BCrypt ne lirait que les 72 premiers

        Reply reply = post("/internal/users",
                registerJson("octets_" + id, "octets_" + id + "@example.com", tropLong), SECRET);

        assertThat(reply.status()).isEqualTo(400);
        assertThat(repository.findByEmail("octets_" + id + "@example.com")).isEmpty();
    }

    @Test
    void impossible_de_se_donner_le_role_admin_a_l_inscription() {
        String id = unique();
        String email = "pirate_" + id + "@example.com";
        String json = "{\"pseudo\":\"pirate_" + id + "\",\"email\":\"" + email + "\",\"password\":\""
                + PASSWORD + "\",\"role\":\"ADMIN\"}";

        post("/internal/users", json, SECRET);   // accepté (champ ignoré) ou refusé : peu importe ici

        // Dans tous les cas, aucun compte ADMIN n'a pu être créé par ce chemin.
        repository.findByEmail(email).ifPresent(u -> assertThat(u.getRole()).isEqualTo(Role.USER));
    }

    // ---------- Vérification des identifiants ----------

    @Test
    void connexion_bon_mot_de_passe_200_mauvais_401_inconnu_401() {
        String id = unique();
        String email = "marc_" + id + "@example.com";
        post("/internal/users", registerJson("marc_" + id, email, PASSWORD), SECRET);

        Reply good = post("/internal/users/verify", verifyJson(email, PASSWORD), SECRET);
        Reply bad = post("/internal/users/verify", verifyJson(email, "mauvais-mot-de-passe"), SECRET);
        Reply unknown = post("/internal/users/verify", verifyJson("inconnu_" + id + "@example.com", PASSWORD), SECRET);

        assertThat(good.status()).isEqualTo(200);
        assertThat(good.body()).contains("marc_" + id).doesNotContain("$2");
        assertThat(bad.status()).isEqualTo(401);
        assertThat(unknown.status()).isEqualTo(401);
    }

    @Test
    void connexion_ignore_les_majuscules_de_l_email() {
        String id = unique();
        post("/internal/users", registerJson("clara_" + id, "clara_" + id + "@example.com", PASSWORD), SECRET);

        Reply reply = post("/internal/users/verify", verifyJson("CLARA_" + id + "@Example.COM", PASSWORD), SECRET);

        assertThat(reply.status()).isEqualTo(200);
    }

    @Test
    void compte_desactive_ne_peut_pas_se_connecter() {
        String id = unique();
        User user = new User(UUID.randomUUID(), "off_" + id, "off_" + id + "@example.com",
                encoder.encode(PASSWORD), Role.USER);
        user.disable();
        repository.saveAndFlush(user);

        Reply reply = post("/internal/users/verify", verifyJson("off_" + id + "@example.com", PASSWORD), SECRET);

        assertThat(reply.status()).isEqualTo(401);
    }

    @Test
    void verification_sans_le_secret_est_refusee() {
        Reply reply = post("/internal/users/verify", verifyJson("a@example.com", PASSWORD), null);

        assertThat(reply.status()).isEqualTo(401);
    }

    // ---------- Santé ----------

    @Test
    void la_sonde_de_sante_repond_sans_secret_et_la_base_est_joignable() {
        Reply reply = get("/actuator/health");

        assertThat(reply.status()).isEqualTo(200);
        assertThat(reply.body()).contains("UP");
    }
}

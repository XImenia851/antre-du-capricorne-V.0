package fr.antrecapricorne.webapp.users;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fr.antrecapricorne.webapp.security.AccountPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Seul point de contact entre la webapp et users-service.
 * La webapp ne touche jamais à la base des comptes : elle passe toujours par ici.
 *
 * <p>Des délais maximum (timeouts) sont réglés : si users-service ne répond pas, la webapp ne
 * reste pas bloquée indéfiniment.</p>
 */
@Component
public class UsersClient {

    private final RestClient client;

    public UsersClient(@Value("${users-service.url}") String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(5));

        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    public RegistrationResult register(String pseudo, String email, String password) {
        try {
            client.post()
                    .uri("/internal/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new RegisterPayload(pseudo, email, password))
                    .retrieve()
                    .toBodilessEntity();
            return RegistrationResult.created();
        } catch (HttpClientErrorException e) {
            int status = e.getStatusCode().value();
            if (status == 409) {
                return RegistrationResult.duplicate();
            }
            if (status == 400) {
                ErrorBody body = e.getResponseBodyAs(ErrorBody.class);
                Map<String, String> errors = (body != null && body.errors() != null) ? body.errors() : Map.of();
                return RegistrationResult.invalid(errors);
            }
            throw new UsersServiceUnavailableException(e);
        } catch (RestClientException e) {
            throw new UsersServiceUnavailableException(e);
        }
    }

    /** Retourne le compte si les identifiants sont bons, sinon vide. */
    public Optional<AccountPrincipal> verify(String email, String password) {
        try {
            UserBody body = client.post()
                    .uri("/internal/users/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new VerifyPayload(email, password))
                    .retrieve()
                    .body(UserBody.class);
            if (body == null) {
                return Optional.empty();
            }
            return Optional.of(new AccountPrincipal(body.id(), body.pseudo(), body.role()));
        } catch (HttpClientErrorException e) {
            // 401 (identifiants refusés) ou 400 (format absurde) : dans les deux cas, on refuse.
            return Optional.empty();
        } catch (RestClientException e) {
            throw new UsersServiceUnavailableException(e);
        }
    }

    // ---- Corps JSON échangés avec users-service ----

    public record RegisterPayload(String pseudo, String email, String password) {
        @Override
        public String toString() {
            return "RegisterPayload[pseudo=" + pseudo + ", email=" + email + ", password=***]";
        }
    }

    public record VerifyPayload(String email, String password) {
        @Override
        public String toString() {
            return "VerifyPayload[email=" + email + ", password=***]";
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserBody(UUID id, String pseudo, String role) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ErrorBody(Map<String, String> errors) {
    }
}

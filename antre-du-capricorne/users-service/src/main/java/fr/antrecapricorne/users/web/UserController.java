package fr.antrecapricorne.users.web;

import fr.antrecapricorne.users.account.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API interne : appelée uniquement par la webapp, à l'intérieur du réseau Docker.
 * (Le secret partagé INTERNAL_SECRET sera vérifié à l'étape 5.)
 */
@RestController
@RequestMapping("/internal/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        var user = service.register(request.pseudo(), request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/verify")
    public ResponseEntity<UserResponse> verify(@Valid @RequestBody VerifyRequest request) {
        return service.verify(request.email(), request.password())
                .map(user -> ResponseEntity.ok(UserResponse.from(user)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}

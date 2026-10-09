package fr.antrecapricorne.users.web;

import fr.antrecapricorne.users.account.DuplicateUserException;
import fr.antrecapricorne.users.account.InvalidPasswordException;
import fr.antrecapricorne.users.account.ValidationRules;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Transforme les erreurs en réponses JSON propres (format standard ProblemDetail).
 * On ne renvoie jamais de trace d'erreur ni la valeur refusée (un mot de passe, par exemple).
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail onValidation(MethodArgumentNotValidException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(fe -> errors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Certains champs sont invalides.");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(DuplicateUserException.class)
    public ProblemDetail onDuplicate(DuplicateUserException e) {
        // Message volontairement vague : ne pas révéler si c'est le pseudo OU l'e-mail qui existe.
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Impossible de créer ce compte avec ces informations.");
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ProblemDetail onInvalidPassword(InvalidPasswordException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Certains champs sont invalides.");
        problem.setProperty("errors", Map.of("password", ValidationRules.PASSWORD_BYTES_MESSAGE));
        return problem;
    }
}

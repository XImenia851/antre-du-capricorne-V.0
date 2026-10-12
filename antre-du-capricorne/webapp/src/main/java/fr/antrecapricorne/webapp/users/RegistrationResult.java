package fr.antrecapricorne.webapp.users;

import java.util.Map;

/** Résultat d'une tentative d'inscription auprès de users-service. */
public record RegistrationResult(Status status, Map<String, String> fieldErrors) {

    public enum Status {
        CREATED, DUPLICATE, INVALID
    }

    public static RegistrationResult created() {
        return new RegistrationResult(Status.CREATED, Map.of());
    }

    public static RegistrationResult duplicate() {
        return new RegistrationResult(Status.DUPLICATE, Map.of());
    }

    public static RegistrationResult invalid(Map<String, String> fieldErrors) {
        return new RegistrationResult(Status.INVALID, fieldErrors);
    }
}

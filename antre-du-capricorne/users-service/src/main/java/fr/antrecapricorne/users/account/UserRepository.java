package fr.antrecapricorne.users.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Accès aux comptes.
 *
 * <p>Les requêtes utilisent {@code lower(...)} des deux côtés pour correspondre exactement aux
 * index {@code ux_users_pseudo_lower} et {@code ux_users_email_lower} de la migration V1 :
 * la base peut alors se servir de l'index. Les paramètres sont toujours liés ({@code :email}),
 * jamais collés dans la requête : c'est ce qui empêche l'injection SQL.</p>
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("select u from User u where lower(u.email) = lower(:email)")
    Optional<User> findByEmail(@Param("email") String email);

    @Query("select count(u) > 0 from User u where lower(u.pseudo) = lower(:pseudo)")
    boolean existsByPseudo(@Param("pseudo") String pseudo);

    @Query("select count(u) > 0 from User u where lower(u.email) = lower(:email)")
    boolean existsByEmail(@Param("email") String email);
}

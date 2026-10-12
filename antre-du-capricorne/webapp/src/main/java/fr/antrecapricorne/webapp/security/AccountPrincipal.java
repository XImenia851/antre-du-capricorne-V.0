package fr.antrecapricorne.webapp.security;

import java.io.Serializable;
import java.util.UUID;

/**
 * L'utilisateur connecté, tel qu'il est gardé dans la session.
 *
 * <p>Règle de sécurité (anti-IDOR) : pour savoir « qui est là », on lit TOUJOURS cet objet,
 * jamais un identifiant envoyé dans l'URL ou dans un formulaire. Sinon, il suffirait de changer
 * un chiffre dans l'URL pour voir les favoris de quelqu'un d'autre.</p>
 *
 * <p>Serializable : la session peut être sauvegardée sur disque ou partagée entre serveurs.</p>
 */
public record AccountPrincipal(UUID id, String pseudo, String role) implements Serializable {
}

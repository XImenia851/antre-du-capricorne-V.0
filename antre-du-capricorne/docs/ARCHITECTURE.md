# L'antre du capricorne — Architecture

> Nom confirmé : **L'antre du capricorne**. Reste à vérifier : disponibilité du nom de domaine et base de marques de l'INPI.
> Document vivant : les points marqués **[À CONFIRMER]** sont des hypothèses à valider.

## 1. Vision et périmètre

Un grimoire de sorcière numérique, en lecture pour tout le monde, avec comptes pour les favoris.

**Dans le périmètre**
- 6 catégories : pierres, plantes, sorts, rituels, astrologie, évènements.
- Page catalogue par catégorie ; clic sur une entrée = pop-up en forme de carte (effets, spirituel, associations).
- Compte utilisateur : inscription, connexion, favoris (visibles uniquement par leur propriétaire).
- Calendrier des évènements sorciers (sabbats, pleines lunes, solstices) avec fils de discussion pour partager ses expériences (phase 2).
- Interface d'administration (cartes, utilisateurs, modération).
- Référencement naturel (SEO) pensé dès le départ.

**Hors périmètre**
- Aucun achat, aucun paiement.
- **Aucun upload de fichier par les utilisateurs** (suite à la faille d'upload relevée sur Makara's Coffee Place).

## 2. Services

```
Navigateur ──HTTPS──► [ webapp ] ──► users-service      (PostgreSQL)
                      Thymeleaf       comptes, rôles, favoris
                      htmx, SEO
                      Spring Security ──► catalog-service    (MongoDB, base "catalog")
                      session             cartes du grimoire

                                      ──► community-service  (MongoDB, base "community")
                                          fils, messages, signalements
```

| Service | Rôle | Données | Exposé à Internet |
|---|---|---|---|
| **webapp** | Vues Thymeleaf, htmx, SEO (sitemap, balises), session, config Spring Security, pages `/admin` | aucune (stateless côté données) | **Oui (seul)** |
| **users-service** | Comptes, mots de passe hashés, rôles, favoris, vérification des identifiants | PostgreSQL | Non |
| **catalog-service** | Lecture et gestion des cartes | MongoDB (base `catalog`) | Non |
| **community-service** | Fils de discussion par évènement, messages, signalements, modération | MongoDB (base `community`) | Non (phase 2) |

## 3. Règles d'architecture

1. **Chaque service possède ses données.** Aucun service ne lit la base d'un autre.
2. **Identifiant commun : le `slug`** (ex. `amethyste`), jamais l'ObjectId Mongo. Il sert aux URLs SEO et aux références entre services.
3. **Favoris** : `users-service` stocke des couples (utilisateur, slug). Pour « Mes favoris », le webapp récupère les slugs puis appelle `catalog-service` en lot (`GET /cards?slugs=a,b,c`). Pas de jointure entre bases.
4. **Seul le webapp publie un port.** Les services internes vivent sur un réseau Docker privé.
5. **Pas de config-server** : un `application.yml` par service ; les secrets passent par variables d'environnement.
6. **Pas de service de découverte (Consul)** : la résolution DNS de Docker Compose suffit.
7. **Pas d'API gateway** : le webapp joue ce rôle tant que le nombre de services reste faible.

## 4. Sécurité

### 4.1 Répartition de l'authentification
- **users-service** : possède les comptes et les mots de passe (hash BCrypt ou Argon2). Expose une vérification d'identifiants. Le mot de passe ne sort jamais de ce service.
- **webapp** : possède la configuration Spring Security (page de login, session, CSRF, règles d'accès, limitation des tentatives). La session ne naît que dans le webapp.
- **Services internes** : configuration Spring Security minimale ; ils refusent tout appel sans le secret interne.

### 4.2 Navigateur → webapp
- Session par cookie `HttpOnly`, `Secure`, `SameSite`. Pas de JWT côté navigateur.
- CSRF activé (y compris sur les requêtes htmx : jeton envoyé en en-tête).
- Limitation des tentatives de connexion (verrouillage temporaire ou délai croissant).
- En-têtes de sécurité : CSP stricte, `X-Content-Type-Options`, `Referrer-Policy`, HSTS en production.
- Échappement systématique des sorties (Thymeleaf `th:text`) ; jamais de `th:utext` sur du contenu issu d'un utilisateur.

### 4.3 Webapp → services internes
- Réseau Docker privé + secret interne partagé (variable d'environnement) vérifié par un filtre dans chaque service. Évolution possible : JWT de service signé.

### 4.4 Autorisation
- Rôles : `USER`, `MODERATOR` (phase 2), `ADMIN`.
- **Contrôle d'appartenance** : un utilisateur ne lit et ne modifie que ses propres favoris. L'identifiant utilisateur vient de la session, **jamais** d'un paramètre d'URL ou de formulaire (protection contre l'IDOR).
- **Le rôle n'est jamais lu depuis un formulaire** (protection contre le mass assignment). Seul un `ADMIN` change le rôle d'un autre compte, jamais le sien.

### 4.5 Validation (REGEX et contraintes)
Toujours côté serveur (Bean Validation), jamais uniquement dans le formulaire.
- `slug` : `^[a-z0-9]+(-[a-z0-9]+)*$`
- Pseudo, email, mot de passe : motifs écrits et expliqués un par un (à rédiger ensemble).
- `resume` : longueur maximale ; `durete` : nombre entre 1 et 10 ; `chakras` : liste fermée de 7 valeurs.

## 5. Modèle d'une carte (catalog-service, MongoDB)

**Champs communs**

| Champ | Détail |
|---|---|
| `slug` | Unique, sert à l'URL |
| `titre` | |
| `categorie` | pierres, plantes, sorts, rituels, astrologie, evenements |
| `resume` | ~2 lignes, max ~160 caractères (réutilisé comme meta description) |
| `type` | Sous-type selon la catégorie (ex. quartz, herbe, sabbat) |
| `effets` | Ce que la carte apporte **[À CONFIRMER : champ séparé de `utilisation`]** |
| `spirituel` | Signification spirituelle |
| `utilisation` | Comment l'utiliser |
| `associations` | Liste de noms/slugs, ex. « quartz », « lavande » **[À CONFIRMER : stockés comme slugs pour pouvoir créer des liens, affichés par leur nom]** |
| `image` | Voir §6 |
| `statut` | `BROUILLON` ou `PUBLIE` |
| `creeLe`, `modifieLe` | Dates |

**Champs optionnels (présents seulement s'ils ont du sens)**
- `durete` (échelle de Mohs) : pierres uniquement.
- `chakras` : liste, uniquement si concerné ; absent (pas vide) sinon.
- Évènements : `date`, `recurrence`.
- Sorts et rituels : `ingredients`, `etapes`.

**Index** : unicité sur `slug` ; index composé `categorie` + `statut` ; index texte sur `titre` et `resume`.

## 6. Images

- Photos libres de droit, **licence vérifiée image par image** (pas site par site).
- Sources possibles : Wikimedia Commons (licence propre à chaque fichier, attribution souvent requise), Unsplash, Pexels, Pixabay (licences propres à chaque site, qui évoluent : relire avant usage).
- **Les images ne sont pas versionnées dans le dépôt de code** (dépôt léger). Chaque carte les référence par une URL.
- **[À CONFIRMER] Emplacement des images** :
  - (a) lien vers le site d'origine : simple, mais dépend du site tiers (image supprimée, lien direct interdit par certains sites, adresse IP du visiteur transmise au tiers) ;
  - (b) copie sur un stockage séparé du code (volume Docker ou stockage objet), non versionnée dans Git : légère pour le code et plus fiable. **Recommandé.**
- **Page « Crédits »** : liste des sites sources et, par image, auteur, licence et lien.
- Champ `image` : `url`, `alt` (utile pour le SEO et l'accessibilité), `auteur`, `source`, `licence`. L'URL est validée (https, domaines autorisés dans une liste blanche) et la CSP `img-src` n'autorise que ces domaines.
- **Aucun formulaire d'upload.**
- Format léger (WebP), tailles adaptées (`srcset`), chargement différé.

## 7. SEO

- **Une vraie page par carte** (`/pierres/amethyste`) ; le pop-up n'est qu'un confort d'affichage au clic depuis le catalogue.
- Le catalogue est public ; seuls les favoris demandent un compte.
- URLs propres en français, titre et meta description uniques par page.
- HTML sémantique (`h1`, `article`, fil d'Ariane).
- Données structurées JSON-LD : `BreadcrumbList`, `Article`, `Event` pour le calendrier.
- `sitemap.xml` généré par le webapp à partir des cartes publiées, `robots.txt`, balise `canonical`, Open Graph.
- Performances (Core Web Vitals) : images optimisées, cache (voir §9).
- Maillage interne via les associations.
- Contenu communautaire : liens externes en `rel="ugc nofollow"`, contenu non modéré non indexé.

## 8. Administration

Routes d'écriture ajoutées dans chaque service, accessibles uniquement via le webapp sous `/admin/**` (rôle `ADMIN`).

| Service | Actions admin |
|---|---|
| catalog-service | Créer, modifier, dépublier, supprimer une carte |
| users-service | Lister les comptes, changer un rôle, désactiver un compte |
| community-service | Traiter les signalements, masquer ou supprimer un message |

- **Le premier admin n'est jamais créé via le site** : injecté au démarrage par variables d'environnement. L'inscription publique ne crée que des `USER`.
- Vérification à deux niveaux : règle `/admin/**` dans le webapp **et** secret interne sur les routes d'écriture des services.
- **Journal d'audit** : qui a fait quoi et quand.
- Cartes en brouillon : invisibles et non indexées.

## 9. Communauté (phase 2)

- Un **fil de discussion par évènement** (pas de forum libre) : plus simple à modérer et fidèle à l'esprit du grimoire.
- Collections `threads` et `posts` (avec `threadId`) : pas de tableau de réponses qui grossit sans limite ; pagination par date.
- Auteur d'un message : identifiant utilisateur + copie du pseudo au moment de l'écriture **[À CONFIRMER]**.
- Anti-abus : authentification requise pour écrire, limite de messages par minute, longueur maximale, bouton « signaler ».
- **Suppression de compte (RGPD)** : le webapp orchestre (suppression dans `users-service`, puis anonymisation dans `community-service`).
- Obligations : mentions légales, politique de confidentialité, gestion des cookies.

## 10. Fiabilité et exploitation

- Timeouts et circuit breaker (Resilience4j) sur les appels du webapp.
- Cache du catalogue (Caffeine + `ETag`) : les cartes changent rarement.
- Healthchecks (Actuator), logs structurés.
- Migrations SQL avec Flyway ; tests avec Testcontainers.
- Docker Compose : un conteneur MongoDB avec deux bases (`catalog`, `community`) et **deux utilisateurs Mongo distincts**, chacun limité à sa base ; un conteneur PostgreSQL.
- Secrets hors du dépôt (variables d'environnement, fichier `.env` ignoré par Git).

## 11. Structure du dépôt **[À CONFIRMER]**

Un seul dépôt, Maven multi-modules :

```
grimoire/
├── pom.xml                 (parent)
├── webapp/
├── users-service/
├── catalog-service/
├── community-service/      (phase 2)
├── docker-compose.yml
├── .env.example
└── docs/
    ├── ARCHITECTURE.md
    └── cahier-des-charges-antre-du-capricorne.pdf
```

## 12. Stack

- Java 21 (LTS), Spring Boot 4.1.1 (version vérifiée le 6 octobre 2026 : la branche 3.5 est hors support open source depuis le 30 juin 2026), Spring Security, Spring Data, Bean Validation, Resilience4j.
- Appels entre services : OpenFeign ou client HTTP déclaratif intégré à Spring (RestClient + HTTP Interface) **[À CONFIRMER]**. OpenFeign dépend de Spring Cloud, dont la compatibilité avec Spring Boot 4.1 reste à vérifier ; le client intégré évite cette dépendance.
- Thymeleaf + htmx + CSS moderne (variables, `@container`, View Transitions) ; JavaScript natif pour le pop-up, les animations et le mode jour/nuit.
- PostgreSQL, MongoDB, Docker Compose.

## 13. Décisions et raisons

| Décision | Raison |
|---|---|
| htmx plutôt qu'une SPA | HTML rendu côté serveur : meilleur SEO, moins de JS exposé, moins de code |
| Pas de service d'authentification séparé | Cinquième service sans gain à cette taille |
| Sécurité de session dans le webapp | C'est là que vit la session HTTP et que les formulaires arrivent |
| Slug comme identifiant commun | URLs SEO et références entre services sans dépendre d'une base |
| Deux bases Mongo distinctes | Respect du principe « chaque service possède ses données » |
| Images sans upload | Supprime la surface d'attaque qui a posé problème sur Makara's Coffee Place |

## 14. Phases

1. Squelette : dépôt multi-modules, Docker Compose, Spring Security, comptes.
2. Catalogue : 6 catégories, cartes, pop-up htmx, SEO de base, favoris.
3. Habillage grimoire (parchemin, typographie, animations, mode jour/nuit).
4. Administration (cartes, utilisateurs, audit).
5. Calendrier des évènements.
6. Fils de discussion et modération.

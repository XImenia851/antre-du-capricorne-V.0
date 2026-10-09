# L'antre du capricorne

Grimoire de sorcière numérique : pierres, plantes, sorts, rituels, astrologie et évènements.
Projet en microservices (Java 21, Spring Boot 4.1, PostgreSQL, MongoDB, Docker Compose).

Documentation : [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) et le cahier des charges (PDF) dans `docs/`.

## Prérequis

- Java 21
- Maven 3.9+
- Docker et Docker Compose

## Démarrer

```bash
# 1. Créer le fichier de secrets (il est ignoré par Git : ne le versionne jamais)
cp .env.example .env
#    puis remplace TOUTES les valeurs :
#      openssl rand -base64 24   -> POSTGRES_PASSWORD
#      openssl rand -hex 32      -> INTERNAL_SECRET

# 2. Construire et lancer
docker compose up --build

# 3. Ouvrir http://localhost:8080
```

## Vérifier que l'isolation fonctionne

```bash
curl.exe -I http://localhost:8080          # PowerShell : curl.exe (curl seul est un alias différent)
curl.exe --max-time 3 http://localhost:8081  # users-service : doit ÉCHOUER (non publié)
docker compose ps                        # seul le webapp doit afficher un port publié (8080)
```

Pour inspecter la base :

```bash
docker compose exec postgres psql -U <POSTGRES_USER> -d users
```

## Tests

```bash
mvn verify
```

## Structure

```
.
├── pom.xml              parent Maven (versions des dépendances)
├── webapp/              vues Thymeleaf, session, sécurité : seul service exposé (port 8080)
├── users-service/       comptes, rôles, favoris (PostgreSQL, port 8081, jamais publié)
├── docker-compose.yml   réseau "internal" (sans accès extérieur) + réseau "edge" (webapp seul)
├── .env.example         modèle des secrets
└── docs/                architecture et cahier des charges
```

## Règles du projet

- **Migrations SQL** : une migration Flyway déjà appliquée ne se modifie jamais ; on ajoute `V2`, `V3`…
- **Secrets** : jamais dans le code ni dans Git, toujours en variables d'environnement.
- **Un seul port publié** : celui du webapp.

## État d'avancement

Phase 1 (squelette) :

- [x] Parent Maven, modules `webapp` et `users-service`
- [x] `docker-compose.yml` (PostgreSQL, réseaux, un seul port publié)
- [x] Migration `V1` : table des utilisateurs
- [x] `users-service` : inscription, hash BCrypt, vérification des identifiants, validation (REGEX)
- [ ] `webapp` : Spring Security (inscription, connexion, session, CSRF)
- [ ] Secret interne entre `webapp` et `users-service`
- [ ] Tests (validation, accès refusé sans connexion, Testcontainers)

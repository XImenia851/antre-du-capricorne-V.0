-- V1 : comptes utilisateurs
-- Règle Flyway : une migration déjà appliquée ne se modifie JAMAIS ; on ajoute V2, V3...

CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    pseudo        VARCHAR(30)  NOT NULL,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,   -- hash BCrypt (60 caractères), jamais le mot de passe
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_users_role CHECK (role IN ('USER', 'MODERATOR', 'ADMIN'))
);

-- Unicité insensible à la casse : "Luna" et "luna" ne peuvent pas coexister.
-- L'index est la vraie barrière : même deux inscriptions simultanées ne peuvent pas la contourner.
CREATE UNIQUE INDEX ux_users_pseudo_lower ON users (lower(pseudo));
CREATE UNIQUE INDEX ux_users_email_lower  ON users (lower(email));

CREATE TABLE IF NOT EXISTS users (
                                     id            BIGSERIAL PRIMARY KEY,
                                     username      VARCHAR(50)  NOT NULL UNIQUE,
    password      VARCHAR(255) NOT NULL,
    status        VARCHAR(10)  NOT NULL DEFAULT 'OFFLINE',
    date_creation TIMESTAMP    NOT NULL DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS messages (
                                        id          BIGSERIAL PRIMARY KEY,
                                        sender_id   BIGINT    NOT NULL REFERENCES users(id),
    receiver_id BIGINT    NOT NULL REFERENCES users(id),
    contenu     VARCHAR(1000) NOT NULL,
    date_envoi  TIMESTAMP    NOT NULL DEFAULT NOW(),
    statut      VARCHAR(10)  NOT NULL DEFAULT 'ENVOYE'
    );
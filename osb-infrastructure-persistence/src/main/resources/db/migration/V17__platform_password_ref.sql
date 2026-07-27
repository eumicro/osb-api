-- Platform Basic-Auth passwords live in SecretStore; Postgres holds only the ref.
ALTER TABLE platform_clients
    ADD COLUMN password_ref TEXT NOT NULL DEFAULT '';

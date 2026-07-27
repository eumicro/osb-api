-- Client credential columns hold SecretStore refs (osb/{type}/{id}/{field}), not plaintext.
-- Existing plaintext rows are migrated to OpenBao/memory by ClientSecretsMigrator at startup.

ALTER TABLE http_client_instances RENAME COLUMN secret TO secret_ref;
ALTER TABLE http_client_instances RENAME COLUMN oauth_client_secret TO oauth_client_secret_ref;

ALTER TABLE git_client_instances RENAME COLUMN secret TO secret_ref;
ALTER TABLE git_client_instances RENAME COLUMN passphrase TO passphrase_ref;

ALTER TABLE kubernetes_client_instances RENAME COLUMN token TO token_ref;
ALTER TABLE kubernetes_client_instances RENAME COLUMN oauth_client_secret TO oauth_client_secret_ref;

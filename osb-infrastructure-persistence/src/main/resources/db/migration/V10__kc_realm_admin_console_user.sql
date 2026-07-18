-- Keycloak 26: realm-admin needs firstName/lastName/email (user profile) or login fails
-- with "Account is not fully set up". Dashboard URL is set in AdminStore to
-- /admin/osb-<instanceId>/console/ (realm console, not master deep-link).

UPDATE templates
SET description = 'POST Admin API create realm osb-<instanceId> with usable realm-admin user (KC26 user profile).',
    content = E'{"method":"POST","path":"/admin/realms","body":{"realm":"osb-${instanceId}","enabled":true,"displayName":"${parameters.displayName}","verifyEmail":false,"loginWithEmailAllowed":true,"registrationAllowed":false,"attributes":{"osb.serviceId":"${serviceId}","osb.planId":"${planId}","osb.instanceId":"${instanceId}"},"users":[{"username":"${parameters.adminUsername}","firstName":"${parameters.adminUsername}","lastName":"Admin","email":"${parameters.adminUsername}@localhost","enabled":true,"emailVerified":true,"requiredActions":[],"credentials":[{"type":"password","value":"${parameters.adminPassword}","temporary":false}],"clientRoles":{"realm-management":["realm-admin"]}}]}}\n'
WHERE id = 'tpl-kc-realm-create';

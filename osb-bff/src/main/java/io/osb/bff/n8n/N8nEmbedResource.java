package io.osb.bff.n8n;

import io.quarkus.oidc.IdToken;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Propagates the BFF Keycloak session into n8n via a short-lived bridge ticket.
 * Iframe loads this same-origin endpoint (session cookie works), then redirects to n8n.
 */
@Path("/bff/n8n-embed")
public class N8nEmbedResource {

    @Inject
    SecurityIdentity identity;

    @Inject
    @IdToken
    JsonWebToken idToken;

    @Inject
    JsonWebToken accessToken;

    @ConfigProperty(name = "osb.bff.n8n.base-url", defaultValue = "http://localhost:5678")
    String n8nBaseUrl;

    @ConfigProperty(name = "osb.bff.n8n.bridge-secret", defaultValue = "osb-n8n-bridge-dev-secret")
    String bridgeSecret;

    @GET
    public Response embed(@QueryParam("returnTo") String returnTo) {
        if (identity == null || identity.isAnonymous()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("login required").build();
        }

        String email = firstClaim("email");
        String preferred = firstClaim("preferred_username");
        if (email == null || email.isBlank()) {
            String principal = identity.getPrincipal().getName();
            email = principal != null && principal.contains("@")
                    ? principal
                    : (preferred != null && !preferred.isBlank() ? preferred : "user") + "@osb.local";
        }

        String given = firstClaim("given_name");
        String family = firstClaim("family_name");
        if ((given == null || given.isBlank()) && preferred != null) {
            given = preferred;
        }
        if (given == null || given.isBlank()) {
            given = "OSB";
        }
        if (family == null || family.isBlank()) {
            family = "User";
        }

        String safeReturnTo = sanitizeReturnTo(returnTo);
        String ticket = N8nSessionTicket.issue(
                new N8nSessionTicket.Payload(
                        email,
                        given,
                        family,
                        N8nSessionTicket.defaultExpiryEpochSeconds()),
                bridgeSecret);

        String base = n8nBaseUrl.endsWith("/")
                ? n8nBaseUrl.substring(0, n8nBaseUrl.length() - 1)
                : n8nBaseUrl;
        String target = base
                + "/auth/oidc/bridge?ticket="
                + URLEncoder.encode(ticket, StandardCharsets.UTF_8)
                + "&returnTo="
                + URLEncoder.encode(safeReturnTo, StandardCharsets.UTF_8);
        return Response.seeOther(URI.create(target)).build();
    }

    private String firstClaim(String name) {
        Object fromId = idToken != null ? idToken.getClaim(name) : null;
        if (fromId != null && !String.valueOf(fromId).isBlank()) {
            return String.valueOf(fromId);
        }
        Object fromAccess = accessToken != null ? accessToken.getClaim(name) : null;
        if (fromAccess != null && !String.valueOf(fromAccess).isBlank()) {
            return String.valueOf(fromAccess);
        }
        return null;
    }

    private static String sanitizeReturnTo(String value) {
        if (value == null || value.isBlank()) {
            return "/";
        }
        String trimmed = value.trim();
        if (!trimmed.startsWith("/") || trimmed.startsWith("//") || trimmed.contains("://")) {
            return "/";
        }
        return trimmed;
    }
}

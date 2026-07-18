package io.osb.bff;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import java.util.Set;

/** Returns the authenticated subject for the admin UI. */
@Path("/bff/me")
@Produces(MediaType.APPLICATION_JSON)
public class MeResource {

    @Inject
    SecurityIdentity identity;

    @GET
    public Map<String, Object> me() {
        if (identity.isAnonymous()) {
            return Map.of(
                    "name", "anonymous",
                    "roles", Set.of());
        }
        return Map.of(
                "name", identity.getPrincipal().getName(),
                "roles", Set.copyOf(identity.getRoles()));
    }
}

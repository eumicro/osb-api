package io.osb.bff.uiconfig;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/bff/ui-config/{key}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UiConfigResource {

    @Inject
    UiConfigStore store;

    @Inject
    SecurityIdentity identity;

    @GET
    public Response get(@PathParam("key") String key) {
        return store.find(userName(), key)
                .map(json -> Response.ok(json).build())
                .orElseThrow(NotFoundException::new);
    }

    @PUT
    public Response save(@PathParam("key") String key, String json) {
        store.save(userName(), key, json);
        return Response.noContent().build();
    }

    @DELETE
    public Response delete(@PathParam("key") String key) {
        if (!store.delete(userName(), key)) {
            throw new NotFoundException();
        }
        return Response.noContent().build();
    }

    private String userName() {
        if (identity.isAnonymous()) {
            return "anonymous";
        }
        return identity.getPrincipal().getName();
    }
}

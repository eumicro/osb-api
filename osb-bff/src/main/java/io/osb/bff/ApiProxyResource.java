package io.osb.bff;

import io.osb.bff.proxy.UpstreamProxy;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.util.Map;

/** Proxies {@code /api/**} to osb-api. */
@Path("/api")
public class ApiProxyResource {

    @Inject
    UpstreamProxy upstreamProxy;

    @GET
    @Path("/{path:.*}")
    public Response get(
            @PathParam("path") String path,
            @Context UriInfo uriInfo,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) String acceptLanguage) {
        return upstreamProxy.get("/api", path, uriInfo, acceptLanguage, Map.of());
    }

    @POST
    @Path("/{path:.*}")
    public Response post(
            @PathParam("path") String path,
            @Context UriInfo uriInfo,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) String acceptLanguage,
            @HeaderParam(HttpHeaders.CONTENT_TYPE) String contentType,
            InputStream body) {
        return upstreamProxy.post("/api", path, uriInfo, acceptLanguage, contentType, body, Map.of());
    }

    @PUT
    @Path("/{path:.*}")
    public Response put(
            @PathParam("path") String path,
            @Context UriInfo uriInfo,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) String acceptLanguage,
            @HeaderParam(HttpHeaders.CONTENT_TYPE) String contentType,
            InputStream body) {
        return upstreamProxy.put("/api", path, uriInfo, acceptLanguage, contentType, body, Map.of());
    }

    @DELETE
    @Path("/{path:.*}")
    public Response delete(
            @PathParam("path") String path,
            @Context UriInfo uriInfo,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) String acceptLanguage,
            @HeaderParam(HttpHeaders.CONTENT_TYPE) String contentType,
            InputStream body) {
        return upstreamProxy.delete("/api", path, uriInfo, acceptLanguage, contentType, body, Map.of());
    }
}

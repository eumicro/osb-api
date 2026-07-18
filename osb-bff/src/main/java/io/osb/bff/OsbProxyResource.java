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

/** Proxies {@code /v2/**} (OSB API surface) to osb-api for the admin UI. */
@Path("/v2")
public class OsbProxyResource {

    private static final String BROKER_API_VERSION = "X-Broker-API-Version";

    @Inject
    UpstreamProxy upstreamProxy;

    @GET
    @Path("/{path:.*}")
    public Response get(
            @PathParam("path") String path,
            @Context UriInfo uriInfo,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) String acceptLanguage,
            @HeaderParam(BROKER_API_VERSION) String brokerApiVersion) {
        return upstreamProxy.get(
                "/v2",
                path,
                uriInfo,
                acceptLanguage,
                Map.of(BROKER_API_VERSION, brokerApiVersion));
    }

    @POST
    @Path("/{path:.*}")
    public Response post(
            @PathParam("path") String path,
            @Context UriInfo uriInfo,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) String acceptLanguage,
            @HeaderParam(HttpHeaders.CONTENT_TYPE) String contentType,
            @HeaderParam(BROKER_API_VERSION) String brokerApiVersion,
            InputStream body) {
        return upstreamProxy.post(
                "/v2",
                path,
                uriInfo,
                acceptLanguage,
                contentType,
                body,
                Map.of(BROKER_API_VERSION, brokerApiVersion));
    }

    @PUT
    @Path("/{path:.*}")
    public Response put(
            @PathParam("path") String path,
            @Context UriInfo uriInfo,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) String acceptLanguage,
            @HeaderParam(HttpHeaders.CONTENT_TYPE) String contentType,
            @HeaderParam(BROKER_API_VERSION) String brokerApiVersion,
            InputStream body) {
        return upstreamProxy.put(
                "/v2",
                path,
                uriInfo,
                acceptLanguage,
                contentType,
                body,
                Map.of(BROKER_API_VERSION, brokerApiVersion));
    }

    @DELETE
    @Path("/{path:.*}")
    public Response delete(
            @PathParam("path") String path,
            @Context UriInfo uriInfo,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) String acceptLanguage,
            @HeaderParam(HttpHeaders.CONTENT_TYPE) String contentType,
            @HeaderParam(BROKER_API_VERSION) String brokerApiVersion,
            InputStream body) {
        return upstreamProxy.delete(
                "/v2",
                path,
                uriInfo,
                acceptLanguage,
                contentType,
                body,
                Map.of(BROKER_API_VERSION, brokerApiVersion));
    }
}

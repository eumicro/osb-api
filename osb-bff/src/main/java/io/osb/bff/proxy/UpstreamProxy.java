package io.osb.bff.proxy;

import io.quarkus.oidc.AccessTokenCredential;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Relays browser calls to osb-api and propagates the OIDC access token when present.
 */
@ApplicationScoped
public class UpstreamProxy {

    @ConfigProperty(name = "osb.bff.api-url")
    String apiUrl;

    @Inject
    Instance<AccessTokenCredential> accessToken;

    private Client client;

    @PostConstruct
    void init() {
        client = ClientBuilder.newClient();
    }

    @PreDestroy
    void destroy() {
        client.close();
    }

    public Response get(
            String prefix,
            String path,
            UriInfo uriInfo,
            String acceptLanguage,
            Map<String, String> extraHeaders) {
        return relay(request(prefix, path, uriInfo, acceptLanguage, extraHeaders).buildGet().invoke());
    }

    public Response post(
            String prefix,
            String path,
            UriInfo uriInfo,
            String acceptLanguage,
            String contentType,
            InputStream body,
            Map<String, String> extraHeaders) {
        return relay(request(prefix, path, uriInfo, acceptLanguage, extraHeaders)
                .buildPost(entity(body, contentType))
                .invoke());
    }

    public Response put(
            String prefix,
            String path,
            UriInfo uriInfo,
            String acceptLanguage,
            String contentType,
            InputStream body,
            Map<String, String> extraHeaders) {
        return relay(request(prefix, path, uriInfo, acceptLanguage, extraHeaders)
                .buildPut(entity(body, contentType))
                .invoke());
    }

    public Response delete(
            String prefix,
            String path,
            UriInfo uriInfo,
            String acceptLanguage,
            String contentType,
            InputStream body,
            Map<String, String> extraHeaders) {
        Invocation.Builder builder = request(prefix, path, uriInfo, acceptLanguage, extraHeaders);
        Invocation invocation = contentType == null
                ? builder.buildDelete()
                : builder.build("DELETE", entity(body, contentType));
        return relay(invocation.invoke());
    }

    private Invocation.Builder request(
            String prefix,
            String path,
            UriInfo uriInfo,
            String acceptLanguage,
            Map<String, String> extraHeaders) {
        String query = uriInfo.getRequestUri().getRawQuery();
        String target = apiUrl + prefix + "/" + path + (query == null ? "" : "?" + query);
        Invocation.Builder builder = client.target(target).request();

        if (accessToken.isResolvable() && accessToken.get().getToken() != null) {
            builder = builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.get().getToken());
        }
        if (acceptLanguage != null) {
            builder = builder.header(HttpHeaders.ACCEPT_LANGUAGE, acceptLanguage);
        }
        if (extraHeaders != null) {
            for (Map.Entry<String, String> entry : extraHeaders.entrySet()) {
                if (entry.getValue() != null) {
                    builder = builder.header(entry.getKey(), entry.getValue());
                }
            }
        }
        return builder;
    }

    private static Entity<InputStream> entity(InputStream body, String contentType) {
        return Entity.entity(body, contentType == null ? MediaType.APPLICATION_JSON : contentType);
    }

    private static Response relay(Response upstream) {
        Response.ResponseBuilder builder = Response.status(upstream.getStatus());
        Object contentType = upstream.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        if (contentType != null) {
            builder.header(HttpHeaders.CONTENT_TYPE, contentType.toString());
        }
        return builder.entity(upstream.readEntity(byte[].class)).build();
    }
}

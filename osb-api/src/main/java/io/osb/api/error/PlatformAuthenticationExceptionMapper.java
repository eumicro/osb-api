package io.osb.api.error;

import io.osb.auth.PlatformAuthenticationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

@Provider
public class PlatformAuthenticationExceptionMapper
        implements ExceptionMapper<PlatformAuthenticationException> {

    @Override
    public Response toResponse(PlatformAuthenticationException exception) {
        return Response.status(Response.Status.UNAUTHORIZED)
                .header("WWW-Authenticate", "Basic realm=\"osb\"")
                .entity(Map.of(
                        "description",
                        exception.getMessage() == null ? "unauthorized" : exception.getMessage()))
                .build();
    }
}

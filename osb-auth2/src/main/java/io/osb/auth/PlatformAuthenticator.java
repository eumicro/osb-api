package io.osb.auth;

/**
 * Port for OSB platform-client authentication (Basic Auth per OSB Spec).
 */
public interface PlatformAuthenticator {

    PlatformPrincipal authenticate(String username, String password);
}

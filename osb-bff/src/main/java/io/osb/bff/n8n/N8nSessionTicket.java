package io.osb.bff.n8n;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Short-lived HMAC ticket so the BFF can propagate the logged-in Keycloak user into n8n
 * without relying on third-party SSO cookies inside an iframe.
 */
public final class N8nSessionTicket {

    private N8nSessionTicket() {
    }

    public record Payload(String email, String firstName, String lastName, long exp) {
    }

    public static String issue(Payload payload, String secret) {
        String body = payload.email()
                + "|"
                + nullToEmpty(payload.firstName())
                + "|"
                + nullToEmpty(payload.lastName())
                + "|"
                + payload.exp();
        String bodyB64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(body.getBytes(StandardCharsets.UTF_8));
        return bodyB64 + "." + sign(bodyB64, secret);
    }

    public static Payload verify(String ticket, String secret, long nowEpochSeconds) {
        if (ticket == null || ticket.isBlank() || !ticket.contains(".")) {
            throw new IllegalArgumentException("invalid ticket");
        }
        int dot = ticket.indexOf('.');
        String bodyB64 = ticket.substring(0, dot);
        String signature = ticket.substring(dot + 1);
        String expected = sign(bodyB64, secret);
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("invalid ticket signature");
        }
        String body = new String(Base64.getUrlDecoder().decode(bodyB64), StandardCharsets.UTF_8);
        String[] parts = body.split("\\|", -1);
        if (parts.length != 4) {
            throw new IllegalArgumentException("invalid ticket payload");
        }
        long exp = Long.parseLong(parts[3]);
        if (exp < nowEpochSeconds) {
            throw new IllegalArgumentException("ticket expired");
        }
        if (parts[0].isBlank() || !parts[0].contains("@")) {
            throw new IllegalArgumentException("invalid email in ticket");
        }
        return new Payload(parts[0], parts[1], parts[2], exp);
    }

    public static long defaultExpiryEpochSeconds() {
        return Instant.now().getEpochSecond() + 120;
    }

    private static String sign(String bodyB64, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(bodyB64.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("ticket signing failed", e);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

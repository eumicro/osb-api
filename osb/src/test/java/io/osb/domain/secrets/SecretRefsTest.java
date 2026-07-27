package io.osb.domain.secrets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SecretRefsTest {

    @Test
    void buildsCanonicalRefs() {
        assertEquals("osb/http/http-abc/secret", SecretRefs.httpSecret("http-abc"));
        assertEquals(
                "osb/http/http-abc/oauthClientSecret",
                SecretRefs.httpOauthClientSecret("http-abc"));
        assertEquals("osb/git/git-demo/secret", SecretRefs.gitSecret("git-demo"));
        assertEquals("osb/git/git-demo/passphrase", SecretRefs.gitPassphrase("git-demo"));
        assertEquals("osb/k8s/k8s-local/token", SecretRefs.k8sToken("k8s-local"));
        assertEquals(
                "osb/k8s/k8s-local/oauthClientSecret",
                SecretRefs.k8sOauthClientSecret("k8s-local"));
        assertEquals("osb/platform/platform-1/password", SecretRefs.platformPassword("platform-1"));
    }

    @Test
    void detectsRefs() {
        assertTrue(SecretRefs.isRef("osb/http/x/secret"));
        assertFalse(SecretRefs.isRef("plaintext"));
        assertFalse(SecretRefs.isRef(""));
        assertFalse(SecretRefs.isRef(null));
    }
}

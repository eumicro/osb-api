package io.osb.infrastructure.kubernetes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import org.junit.jupiter.api.Test;

class KubernetesResourceReadinessTest {

    @Test
    void deploymentNotReadyUntilReadyReplicasMatch() {
        var notReady = new DeploymentBuilder()
                .withNewMetadata()
                .withName("api")
                .endMetadata()
                .withNewSpec()
                .withReplicas(1)
                .endSpec()
                .withNewStatus()
                .withReadyReplicas(0)
                .endStatus()
                .build();
        assertFalse(KubernetesResourceReadiness.isReady(notReady));
        assertEquals("notReady", KubernetesResourceReadiness.label(notReady));

        var ready = new DeploymentBuilder(notReady)
                .editStatus()
                .withReadyReplicas(1)
                .endStatus()
                .build();
        assertTrue(KubernetesResourceReadiness.isReady(ready));
        assertEquals("ready", KubernetesResourceReadiness.label(ready));
    }

    @Test
    void serviceIsReadyWhenPresent() {
        var service = new ServiceBuilder()
                .withNewMetadata()
                .withName("api")
                .endMetadata()
                .build();
        assertTrue(KubernetesResourceReadiness.isReady(service));
    }

    @Test
    void missingResourceIsNotReady() {
        assertFalse(KubernetesResourceReadiness.isReady(null));
        assertEquals("missing", KubernetesResourceReadiness.label(null));
    }
}

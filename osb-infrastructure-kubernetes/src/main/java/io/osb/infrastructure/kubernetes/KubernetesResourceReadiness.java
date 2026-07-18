package io.osb.infrastructure.kubernetes;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.batch.v1.Job;

/**
 * Workload readiness for OSB {@code get} / last-operation. Non-workload resources are ready when
 * they exist.
 */
final class KubernetesResourceReadiness {

    private KubernetesResourceReadiness() {}

    static boolean isReady(HasMetadata resource) {
        if (resource == null) {
            return false;
        }
        if (resource instanceof Deployment deployment) {
            return deploymentReady(deployment);
        }
        if (resource instanceof StatefulSet statefulSet) {
            return statefulSetReady(statefulSet);
        }
        if (resource instanceof DaemonSet daemonSet) {
            return daemonSetReady(daemonSet);
        }
        if (resource instanceof Job job) {
            return jobReady(job);
        }
        return true;
    }

    /** Status token for logs / details: {@code missing}, {@code ready}, or {@code notReady}. */
    static String label(HasMetadata current) {
        if (current == null) {
            return "missing";
        }
        return isReady(current) ? "ready" : "notReady";
    }

    private static boolean deploymentReady(Deployment deployment) {
        int desired = replicas(deployment.getSpec() == null ? null : deployment.getSpec().getReplicas());
        if (desired == 0) {
            return true;
        }
        if (deployment.getStatus() == null) {
            return false;
        }
        Integer ready = deployment.getStatus().getReadyReplicas();
        return ready != null && ready >= desired;
    }

    private static boolean statefulSetReady(StatefulSet statefulSet) {
        int desired = replicas(statefulSet.getSpec() == null ? null : statefulSet.getSpec().getReplicas());
        if (desired == 0) {
            return true;
        }
        if (statefulSet.getStatus() == null) {
            return false;
        }
        Integer ready = statefulSet.getStatus().getReadyReplicas();
        return ready != null && ready >= desired;
    }

    private static boolean daemonSetReady(DaemonSet daemonSet) {
        if (daemonSet.getStatus() == null) {
            return false;
        }
        Integer desired = daemonSet.getStatus().getDesiredNumberScheduled();
        Integer ready = daemonSet.getStatus().getNumberReady();
        if (desired == null || desired == 0) {
            return true;
        }
        return ready != null && ready >= desired;
    }

    private static boolean jobReady(Job job) {
        if (job.getStatus() == null) {
            return false;
        }
        Integer succeeded = job.getStatus().getSucceeded();
        return succeeded != null && succeeded > 0;
    }

    private static int replicas(Integer value) {
        return value == null ? 1 : value;
    }
}

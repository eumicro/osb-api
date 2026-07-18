package io.osb.domain.templates;

/**
 * Categorization for admin/UI; content is always free text with placeholders.
 */
public enum TemplateKind {
    /** e.g. Kubernetes YAML manifests */
    KUBERNETES_RESOURCE,
    /** e.g. HTTP request body / URL / headers as text */
    HTTP_REQUEST,
    /** e.g. git clone/checkout command lines */
    GIT_COMMAND,
    /** any other placeholder text */
    TEXT
}

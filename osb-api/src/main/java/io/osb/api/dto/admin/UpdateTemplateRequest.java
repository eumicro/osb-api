package io.osb.api.dto.admin;

public record UpdateTemplateRequest(
        String name, String description, String kind, String content, Boolean enabled) {}

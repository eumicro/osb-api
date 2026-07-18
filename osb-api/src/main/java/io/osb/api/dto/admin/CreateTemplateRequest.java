package io.osb.api.dto.admin;

public record CreateTemplateRequest(
        String name, String description, String kind, String content, Boolean enabled) {}

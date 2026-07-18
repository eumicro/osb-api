package io.osb.api.dto.admin;

public record TemplateDto(
        String id,
        String name,
        String description,
        String kind,
        String content,
        boolean enabled) {}

package ru.mirea.appliancerepair.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class ApplianceTypeDtos {
    private ApplianceTypeDtos() { }

    public record ApplianceTypeRequest(
            @NotBlank @Size(max = 50) String code,
            @NotBlank @Size(max = 120) String name,
            @Size(max = 500) String description,
            Boolean active,
            Long version) { }

    public record ApplianceTypeResponse(
            UUID id, String code, String name, String description, boolean active,
            Instant createdAt, Instant updatedAt, long version) { }
}

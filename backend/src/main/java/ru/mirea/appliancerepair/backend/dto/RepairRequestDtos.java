package ru.mirea.appliancerepair.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.mirea.appliancerepair.backend.domain.RepairRisk;
import ru.mirea.appliancerepair.backend.domain.RequestPriority;
import ru.mirea.appliancerepair.backend.domain.RequestStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class RepairRequestDtos {
    private RepairRequestDtos() { }

    public record RepairRequestRequest(
            @Size(max = 100) String externalId,
            @NotNull Long customerId,
            @NotBlank @Size(max = 80) String applianceType,
            UUID applianceTypeId,
            @NotBlank @Size(max = 80) String applianceBrand,
            @NotBlank @Size(max = 200) String issueTitle,
            @NotBlank @Size(max = 1000) String issueDescription,
            @NotNull RequestStatus status,
            @NotNull RequestPriority priority,
            @NotNull @DecimalMin("0.00") @DecimalMax("1000000.00") BigDecimal estimatedCost,
            LocalDate plannedDate,
            boolean urgent,
            @Size(max = 20) Set<@NotBlank @Size(max = 50) String> tags,
            Long version) { }

    public record RepairRequestResponse(
            UUID id,
            String requestNumber,
            String externalId,
            Long customerId,
            String customerName,
            String customerPhone,
            String customerEmail,
            String customerAddress,
            UUID applianceTypeId,
            String applianceType,
            String applianceBrand,
            String issueTitle,
            String issueDescription,
            RequestStatus status,
            RequestPriority priority,
            BigDecimal estimatedCost,
            LocalDate plannedDate,
            boolean urgent,
            RepairRisk riskLevel,
            boolean archived,
            LocalDateTime archivedAt,
            Set<String> tags,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            long version) { }

    public enum BulkAction { ARCHIVE, RESTORE, SET_STATUS, ADD_TAG, REMOVE_TAG }

    public record BulkRequest(
            @NotNull @Size(min = 1, max = 500) Set<UUID> ids,
            @NotNull BulkAction action,
            RequestStatus status,
            @Size(max = 50) String tag) { }

    public record BulkResult(int requested, int succeeded, List<BulkFailure> failures) { }
    public record BulkFailure(UUID id, String message) { }
}

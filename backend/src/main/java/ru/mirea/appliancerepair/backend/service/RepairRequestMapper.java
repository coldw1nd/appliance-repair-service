package ru.mirea.appliancerepair.backend.service;

import org.springframework.stereotype.Component;
import ru.mirea.appliancerepair.backend.domain.RepairRequest;
import ru.mirea.appliancerepair.backend.dto.RepairRequestDtos.RepairRequestResponse;

import java.util.Set;

@Component
public class RepairRequestMapper {
    public RepairRequestResponse toResponse(RepairRequest request) {
        var customer = request.getCustomer();
        return new RepairRequestResponse(
                request.getId(), request.getRequestNumber(), request.getExternalId(), customer.getId(),
                customer.getFullName(), customer.getPhone(), customer.getEmail(), customer.getAddress(),
                request.getApplianceType() == null ? null : request.getApplianceType().getId(),
                request.getApplianceTypeName(), request.getApplianceBrand(), request.getIssueTitle(),
                request.getIssueDescription(), request.getStatus(), request.getPriority(), request.getEstimatedCost(),
                request.getPlannedDate(), request.isUrgent(), request.getRiskLevel(), request.isArchived(),
                request.getArchivedAt(), Set.copyOf(request.getTags()), request.getCreatedAt(),
                request.getUpdatedAt(), request.getVersion());
    }
}

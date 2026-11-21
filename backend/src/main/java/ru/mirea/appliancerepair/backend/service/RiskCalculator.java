package ru.mirea.appliancerepair.backend.service;

import org.springframework.stereotype.Component;
import ru.mirea.appliancerepair.backend.domain.RepairRisk;
import ru.mirea.appliancerepair.backend.domain.RequestPriority;
import ru.mirea.appliancerepair.backend.domain.RequestStatus;

@Component
public class RiskCalculator {
    public RepairRisk calculate(RequestStatus status, RequestPriority priority, boolean urgent, boolean overdue) {
        if (status == RequestStatus.COMPLETED || status == RequestStatus.CANCELLED) {
            return RepairRisk.NOT_APPLICABLE;
        }
        if (urgent || priority == RequestPriority.URGENT) return RepairRisk.CRITICAL;
        if (priority == RequestPriority.HIGH || overdue) return RepairRisk.HIGH;
        if (priority == RequestPriority.MEDIUM) return RepairRisk.MEDIUM;
        return RepairRisk.LOW;
    }
}

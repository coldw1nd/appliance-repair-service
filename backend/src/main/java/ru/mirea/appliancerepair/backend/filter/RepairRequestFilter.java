package ru.mirea.appliancerepair.backend.filter;

import ru.mirea.appliancerepair.backend.domain.RepairRisk;
import ru.mirea.appliancerepair.backend.domain.RequestPriority;
import ru.mirea.appliancerepair.backend.domain.RequestStatus;

import java.math.BigDecimal;
import java.util.List;

public record RepairRequestFilter(
        String freeText,
        String applianceType,
        String customer,
        RequestStatus status,
        RequestPriority priority,
        Boolean urgent,
        RepairRisk risk,
        String tag,
        Boolean archived,
        List<CostConstraint> costs) {

    public enum Operator { LT, LTE, EQ, GTE, GT }
    public record CostConstraint(Operator operator, BigDecimal value) { }
}

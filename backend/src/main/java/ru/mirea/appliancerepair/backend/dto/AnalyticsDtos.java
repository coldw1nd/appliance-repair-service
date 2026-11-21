package ru.mirea.appliancerepair.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public final class AnalyticsDtos {
    private AnalyticsDtos() { }
    public record NamedCount(String name, long count) { }
    public record AnalyticsResponse(long total, long active, long archived, long urgent,
                                    BigDecimal averageEstimatedCost,
                                    List<NamedCount> byApplianceType,
                                    List<NamedCount> byStatus,
                                    List<NamedCount> byRisk) { }
}

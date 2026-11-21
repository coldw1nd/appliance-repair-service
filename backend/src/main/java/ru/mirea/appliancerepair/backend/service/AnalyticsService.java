package ru.mirea.appliancerepair.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.appliancerepair.backend.dto.AnalyticsDtos.AnalyticsResponse;
import ru.mirea.appliancerepair.backend.dto.AnalyticsDtos.NamedCount;
import ru.mirea.appliancerepair.backend.repository.RepairRequestRepository;

import java.math.RoundingMode;
import java.util.List;

@Service
public class AnalyticsService {
    private final RepairRequestRepository requests;
    public AnalyticsService(RepairRequestRepository requests) { this.requests = requests; }

    @Transactional(readOnly = true)
    public AnalyticsResponse summary() {
        var average = requests.averageCostForActive();
        if (average != null) average = average.setScale(2, RoundingMode.HALF_UP);
        return new AnalyticsResponse(requests.count(), requests.countByArchivedFalse(), requests.countByArchivedTrue(),
                requests.countByArchivedFalseAndUrgentTrue(), average,
                map(requests.countActiveByApplianceType()), map(requests.countActiveByStatus()), map(requests.countActiveByRisk()));
    }

    private static List<NamedCount> map(List<Object[]> rows) {
        return rows.stream().map(row -> new NamedCount(String.valueOf(row[0]), ((Number) row[1]).longValue())).toList();
    }
}

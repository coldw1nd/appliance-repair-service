package ru.mirea.appliancerepair.backend.service;

import org.junit.jupiter.api.Test;
import ru.mirea.appliancerepair.backend.domain.RepairRisk;
import ru.mirea.appliancerepair.backend.domain.RequestPriority;
import ru.mirea.appliancerepair.backend.domain.RequestStatus;

import static org.assertj.core.api.Assertions.assertThat;

class RiskCalculatorTest {
    private final RiskCalculator calculator = new RiskCalculator();

    @Test
    void urgentRequestIsCritical() {
        assertThat(calculator.calculate(RequestStatus.REPAIRING, RequestPriority.HIGH, true, false))
                .isEqualTo(RepairRisk.CRITICAL);
    }

    @Test
    void highPriorityRequestHasHighRisk() {
        assertThat(calculator.calculate(RequestStatus.DIAGNOSING, RequestPriority.HIGH, false, false))
                .isEqualTo(RepairRisk.HIGH);
    }

    @Test
    void completedRequestHasNoActiveRisk() {
        assertThat(calculator.calculate(RequestStatus.COMPLETED, RequestPriority.HIGH, true, false))
                .isEqualTo(RepairRisk.NOT_APPLICABLE);
    }
}

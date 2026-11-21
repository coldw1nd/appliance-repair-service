package ru.mirea.appliancerepair.backend.filter;

import org.junit.jupiter.api.Test;
import ru.mirea.appliancerepair.backend.domain.RepairRisk;
import ru.mirea.appliancerepair.backend.domain.RequestPriority;
import ru.mirea.appliancerepair.backend.domain.RequestStatus;
import ru.mirea.appliancerepair.backend.error.BadRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SmartFilterParserTest {
    private final SmartFilterParser parser = new SmartFilterParser();

    @Test
    void parsesRussianAndEnglishFilters() {
        RepairRequestFilter result = parser.parse("не сливает воду тип:Холодильник статус:диагностика приоритет:высокий срочно:да риск:critical тег:гарантия стоимость>5000");
        assertThat(result.freeText()).isEqualTo("не сливает воду");
        assertThat(result.applianceType()).isEqualTo("Холодильник");
        assertThat(result.status()).isEqualTo(RequestStatus.DIAGNOSING);
        assertThat(result.priority()).isEqualTo(RequestPriority.HIGH);
        assertThat(result.urgent()).isTrue();
        assertThat(result.risk()).isEqualTo(RepairRisk.CRITICAL);
        assertThat(result.tag()).isEqualTo("гарантия");
        assertThat(result.archived()).isFalse();
        assertThat(result.costs()).hasSize(1);
    }

    @Test
    void keepsQuotedSearchPhrase() {
        RepairRequestFilter result = parser.parse("\"не охлаждает\" archived:true");
        assertThat(result.freeText()).isEqualTo("не охлаждает");
        assertThat(result.archived()).isTrue();
    }

    @Test
    void canIncludeBothActiveAndArchivedRequests() {
        RepairRequestFilter result = parser.parse("архив:все");
        assertThat(result.archived()).isNull();
    }

    @Test
    void rejectsUnknownStatus() {
        assertThatThrownBy(() -> parser.parse("status:unknown"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsOutOfRangeCost() {
        assertThatThrownBy(() -> parser.parse("стоимость>1000001"))
                .isInstanceOf(BadRequestException.class);
    }
}

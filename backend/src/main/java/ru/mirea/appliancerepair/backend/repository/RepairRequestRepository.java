package ru.mirea.appliancerepair.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import ru.mirea.appliancerepair.backend.domain.RepairRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepairRequestRepository extends JpaRepository<RepairRequest, UUID>, JpaSpecificationExecutor<RepairRequest> {
    boolean existsByExternalIdIgnoreCase(String externalId);
    boolean existsByExternalIdIgnoreCaseAndIdNot(String externalId, UUID id);
    Optional<RepairRequest> findByExternalIdIgnoreCase(String externalId);
    long countByArchivedFalse();
    long countByArchivedTrue();
    long countByArchivedFalseAndUrgentTrue();

    @Query("select coalesce(avg(r.estimatedCost), 0) from RepairRequest r where r.archived = false")
    BigDecimal averageCostForActive();

    @Query("select coalesce(t.name, r.applianceTypeName), count(r) from RepairRequest r " +
            "left join r.applianceType t where r.archived = false group by coalesce(t.name, r.applianceTypeName) order by count(r) desc")
    List<Object[]> countActiveByApplianceType();

    @Query("select r.status, count(r) from RepairRequest r where r.archived = false group by r.status order by count(r) desc")
    List<Object[]> countActiveByStatus();

    @Query("select r.riskLevel, count(r) from RepairRequest r where r.archived = false group by r.riskLevel order by count(r) desc")
    List<Object[]> countActiveByRisk();
}

package ru.mirea.appliancerepair.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.appliancerepair.backend.domain.ApplianceType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplianceTypeRepository extends JpaRepository<ApplianceType, UUID> {
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByNameIgnoreCase(String name);
    Optional<ApplianceType> findByCodeIgnoreCase(String code);
    Optional<ApplianceType> findByNameIgnoreCase(String name);
    List<ApplianceType> findAllByOrderByNameAsc();
    List<ApplianceType> findByActiveTrueOrderByNameAsc();
}

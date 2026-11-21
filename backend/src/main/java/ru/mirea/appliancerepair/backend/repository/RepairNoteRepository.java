package ru.mirea.appliancerepair.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.appliancerepair.backend.domain.RepairNote;

import java.util.List;
import java.util.UUID;

public interface RepairNoteRepository extends JpaRepository<RepairNote, UUID> {
    List<RepairNote> findByRepairRequestIdOrderByPinnedDescCreatedAtDesc(UUID repairRequestId);
}

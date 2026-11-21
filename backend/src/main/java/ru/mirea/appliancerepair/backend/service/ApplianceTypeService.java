package ru.mirea.appliancerepair.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.appliancerepair.backend.domain.ApplianceType;
import ru.mirea.appliancerepair.backend.dto.ApplianceTypeDtos.ApplianceTypeRequest;
import ru.mirea.appliancerepair.backend.dto.ApplianceTypeDtos.ApplianceTypeResponse;
import ru.mirea.appliancerepair.backend.error.ConflictException;
import ru.mirea.appliancerepair.backend.error.NotFoundException;
import ru.mirea.appliancerepair.backend.repository.ApplianceTypeRepository;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class
ApplianceTypeService {
    private final ApplianceTypeRepository types;
    private final AuditService audit;

    public ApplianceTypeService(ApplianceTypeRepository types, AuditService audit) {
        this.types = types;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<ApplianceTypeResponse> list(boolean activeOnly) {
        return (activeOnly ? types.findByActiveTrueOrderByNameAsc() : types.findAllByOrderByNameAsc())
                .stream().map(ApplianceTypeService::toResponse).toList();
    }

    @Transactional
    public ApplianceTypeResponse create(ApplianceTypeRequest request) {
        String code = normalizeCode(request.code());
        String name = normalizeName(request.name());
        if (types.existsByCodeIgnoreCase(code)) throw new ConflictException("Тип техники с таким кодом уже существует");
        if (types.existsByNameIgnoreCase(name)) throw new ConflictException("Тип техники с таким названием уже существует");
        ApplianceType entity = new ApplianceType(UUID.randomUUID(), code, name, normalizeNullable(request.description()));
        if (request.active() != null) entity.setActive(request.active());
        types.save(entity);
        audit.record("APPLIANCE_TYPE_CREATED", "APPLIANCE_TYPE", entity.getId(), "Создан тип техники " + name, Map.of());
        return toResponse(entity);
    }

    @Transactional
    public ApplianceTypeResponse update(UUID id, ApplianceTypeRequest request) {
        ApplianceType entity = require(id);
        verifyVersion(request.version(), entity.getVersion());
        String code = normalizeCode(request.code());
        String name = normalizeName(request.name());
        types.findByCodeIgnoreCase(code).filter(other -> !other.getId().equals(id)).ifPresent(other -> {
            throw new ConflictException("Тип техники с таким кодом уже существует");
        });
        if (types.existsByNameIgnoreCase(name) && !entity.getName().equalsIgnoreCase(name))
            throw new ConflictException("Тип техники с таким названием уже существует");
        entity.setCode(code);
        entity.setName(name);
        entity.setDescription(normalizeNullable(request.description()));
        if (request.active() != null) entity.setActive(request.active());
        audit.record("APPLIANCE_TYPE_UPDATED", "APPLIANCE_TYPE", id, "Изменён тип техники " + name, Map.of());
        return toResponse(entity);
    }

    @Transactional
    public ApplianceTypeResponse setActive(UUID id, boolean active, long version) {
        ApplianceType entity = require(id);
        verifyVersion(version, entity.getVersion());
        entity.setActive(active);
        audit.record(active ? "APPLIANCE_TYPE_ACTIVATED" : "APPLIANCE_TYPE_DEACTIVATED",
                "APPLIANCE_TYPE", id, (active ? "Включён" : "Отключён") + " тип техники " + entity.getName(), Map.of());
        return toResponse(entity);
    }

    private ApplianceType require(UUID id) {
        return types.findById(id).orElseThrow(() -> new NotFoundException("Тип техники не найден"));
    }

    private static void verifyVersion(Long requested, long actual) {
        if (requested == null || requested != actual) throw new ConflictException("Справочник уже изменён. Обновите данные");
    }

    private static ApplianceTypeResponse toResponse(ApplianceType entity) {
        return new ApplianceTypeResponse(entity.getId(), entity.getCode(), entity.getName(), entity.getDescription(),
                entity.isActive(), entity.getCreatedAt(), entity.getUpdatedAt(), entity.getVersion());
    }

    private static String normalizeCode(String value) {
        return value.trim().replaceAll("\\s+", "_").toUpperCase(Locale.ROOT);
    }
    private static String normalizeName(String value) { return value.trim().replaceAll("\\s+", " "); }
    private static String normalizeNullable(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}

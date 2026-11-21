package ru.mirea.appliancerepair.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.appliancerepair.backend.config.AppProperties;
import ru.mirea.appliancerepair.backend.domain.ApplianceType;
import ru.mirea.appliancerepair.backend.domain.Customer;
import ru.mirea.appliancerepair.backend.domain.RepairRequest;
import ru.mirea.appliancerepair.backend.domain.UserAccount;
import ru.mirea.appliancerepair.backend.dto.RepairRequestDtos.BulkAction;
import ru.mirea.appliancerepair.backend.dto.RepairRequestDtos.BulkFailure;
import ru.mirea.appliancerepair.backend.dto.RepairRequestDtos.BulkRequest;
import ru.mirea.appliancerepair.backend.dto.RepairRequestDtos.BulkResult;
import ru.mirea.appliancerepair.backend.dto.RepairRequestDtos.RepairRequestRequest;
import ru.mirea.appliancerepair.backend.dto.RepairRequestDtos.RepairRequestResponse;
import ru.mirea.appliancerepair.backend.error.BadRequestException;
import ru.mirea.appliancerepair.backend.error.ConflictException;
import ru.mirea.appliancerepair.backend.error.NotFoundException;
import ru.mirea.appliancerepair.backend.filter.RepairRequestSpecifications;
import ru.mirea.appliancerepair.backend.filter.SmartFilterParser;
import ru.mirea.appliancerepair.backend.repository.ApplianceTypeRepository;
import ru.mirea.appliancerepair.backend.repository.CustomerRepository;
import ru.mirea.appliancerepair.backend.repository.RepairRequestRepository;
import ru.mirea.appliancerepair.backend.security.CurrentUserService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class RepairRequestService {
    private final RepairRequestRepository requests;
    private final CustomerRepository customers;
    private final ApplianceTypeRepository applianceTypes;
    private final CurrentUserService currentUser;
    private final RepairRequestMapper mapper;
    private final RiskCalculator riskCalculator;
    private final SmartFilterParser filterParser;
    private final AuditService audit;
    private final AppProperties properties;

    public RepairRequestService(RepairRequestRepository requests, CustomerRepository customers,
                                ApplianceTypeRepository applianceTypes, CurrentUserService currentUser,
                                RepairRequestMapper mapper, RiskCalculator riskCalculator,
                                SmartFilterParser filterParser, AuditService audit, AppProperties properties) {
        this.requests = requests;
        this.customers = customers;
        this.applianceTypes = applianceTypes;
        this.currentUser = currentUser;
        this.mapper = mapper;
        this.riskCalculator = riskCalculator;
        this.filterParser = filterParser;
        this.audit = audit;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public Page<RepairRequestResponse> search(String query, Pageable pageable) {
        return requests.findAll(RepairRequestSpecifications.from(filterParser.parse(query)), pageable)
                .map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public RepairRequestResponse get(UUID id) {
        return mapper.toResponse(require(id));
    }

    @Transactional
    public RepairRequestResponse create(RepairRequestRequest request) {
        UserAccount actor = currentUser.requireEntity();
        validateExternalId(request.externalId(), null);
        Customer customer = resolveCustomer(request.customerId());
        ApplianceType type = resolveType(request.applianceTypeId(), request.applianceType());
        RepairRequest entity = new RepairRequest(UUID.randomUUID(), createNumber(), customer,
                normalizeRequired(request.applianceType()), type, normalizeRequired(request.applianceBrand()),
                normalizeRequired(request.issueTitle()), normalizeRequired(request.issueDescription()), actor);
        apply(entity, request, actor);
        requests.save(entity);
        audit.record("REPAIR_REQUEST_CREATED", "REPAIR_REQUEST", entity.getId(),
                "Создана заявка " + entity.getRequestNumber(), Map.of("requestNumber", entity.getRequestNumber()));
        return mapper.toResponse(entity);
    }

    @Transactional
    public RepairRequestResponse update(UUID id, RepairRequestRequest request) {
        RepairRequest entity = require(id);
        verifyVersion(request.version(), entity.getVersion());
        validateExternalId(request.externalId(), id);
        UserAccount actor = currentUser.requireEntity();
        apply(entity, request, actor);
        audit.record("REPAIR_REQUEST_UPDATED", "REPAIR_REQUEST", id,
                "Изменена заявка " + entity.getRequestNumber(), Map.of("version", entity.getVersion()));
        return mapper.toResponse(entity);
    }

    @Transactional
    public RepairRequestResponse archive(UUID id) {
        RepairRequest entity = require(id);
        if (!entity.isArchived()) {
            archiveInternal(entity, currentUser.requireEntity());
            audit.record("REPAIR_REQUEST_ARCHIVED", "REPAIR_REQUEST", id,
                    "Заявка " + entity.getRequestNumber() + " помещена в архив", Map.of());
        }
        return mapper.toResponse(entity);
    }

    @Transactional
    public RepairRequestResponse restore(UUID id) {
        RepairRequest entity = require(id);
        entity.setArchived(false);
        entity.setArchivedAt(null);
        entity.setArchivedBy(null);
        entity.setUpdatedBy(currentUser.requireEntity());
        audit.record("REPAIR_REQUEST_RESTORED", "REPAIR_REQUEST", id,
                "Заявка " + entity.getRequestNumber() + " восстановлена из архива", Map.of());
        return mapper.toResponse(entity);
    }

    @Transactional
    public void permanentDelete(UUID id, String confirmation) {
        if (!properties.permanentDeleteEnabled())
            throw new BadRequestException("Безвозвратное удаление отключено настройками сервера");
        if (!"PERMANENTLY_DELETE".equals(confirmation))
            throw new BadRequestException("Передайте заголовок X-Confirm-Delete: PERMANENTLY_DELETE");
        RepairRequest entity = require(id);
        UUID requestId = entity.getId();
        String number = entity.getRequestNumber();
        requests.delete(entity);
        audit.record("REPAIR_REQUEST_PERMANENTLY_DELETED", "REPAIR_REQUEST", requestId,
                "Безвозвратно удалена заявка " + number, Map.of());
    }

    @Transactional
    public BulkResult bulk(BulkRequest request) {
        if (request.action() == BulkAction.SET_STATUS && request.status() == null)
            throw new BadRequestException("Для SET_STATUS требуется status");
        if ((request.action() == BulkAction.ADD_TAG || request.action() == BulkAction.REMOVE_TAG)
                && (request.tag() == null || request.tag().isBlank()))
            throw new BadRequestException("Для операции с тегом требуется tag");
        UserAccount actor = currentUser.requireEntity();
        int succeeded = 0;
        var failures = new ArrayList<BulkFailure>();
        for (UUID id : request.ids()) {
            RepairRequest entity = requests.findById(id).orElse(null);
            if (entity == null) {
                failures.add(new BulkFailure(id, "Заявка не найдена"));
                continue;
            }
            try {
                applyBulk(entity, request, actor);
                succeeded++;
            } catch (RuntimeException ex) {
                failures.add(new BulkFailure(id, ex.getMessage() == null ? "Операция не выполнена" : ex.getMessage()));
            }
        }
        audit.record("REPAIR_REQUEST_BULK_" + request.action().name(), "REPAIR_REQUEST", null,
                "Выполнена массовая операция с заявками", Map.of("requested", request.ids().size(), "succeeded", succeeded));
        return new BulkResult(request.ids().size(), succeeded, failures);
    }

    RepairRequest upsertFromImport(RepairRequest existing, RepairRequestRequest request, UserAccount actor) {
        validateExternalId(request.externalId(), existing == null ? null : existing.getId());
        if (existing == null) {
            RepairRequest created = new RepairRequest(UUID.randomUUID(), createNumber(), resolveCustomer(request.customerId()),
                    normalizeRequired(request.applianceType()), resolveType(request.applianceTypeId(), request.applianceType()),
                    normalizeRequired(request.applianceBrand()), normalizeRequired(request.issueTitle()),
                    normalizeRequired(request.issueDescription()), actor);
            apply(created, request, actor);
            return requests.save(created);
        }
        apply(existing, request, actor);
        return existing;
    }

    private void apply(RepairRequest entity, RepairRequestRequest request, UserAccount actor) {
        Customer customer = resolveCustomer(request.customerId());
        ApplianceType type = resolveType(request.applianceTypeId(), request.applianceType());
        String applianceTypeName = normalizeRequired(request.applianceType());
        if (request.urgent() && request.priority() != ru.mirea.appliancerepair.backend.domain.RequestPriority.HIGH
                && request.priority() != ru.mirea.appliancerepair.backend.domain.RequestPriority.URGENT) {
            throw new BadRequestException("Для срочной заявки требуется приоритет HIGH или URGENT");
        }
        entity.setExternalId(normalizeNullable(request.externalId()));
        entity.setCustomer(customer);
        entity.setApplianceType(type);
        entity.setApplianceTypeName(applianceTypeName);
        entity.setApplianceBrand(normalizeRequired(request.applianceBrand()));
        entity.setIssueTitle(normalizeRequired(request.issueTitle()));
        entity.setIssueDescription(normalizeRequired(request.issueDescription()));
        entity.setStatus(request.status());
        entity.setPriority(request.priority());
        entity.setEstimatedCost(request.estimatedCost());
        entity.setPlannedDate(request.plannedDate());
        entity.setUrgent(request.urgent());
        entity.setTags(normalizeTags(request.tags()));
        boolean overdue = request.plannedDate() != null && request.plannedDate().isBefore(LocalDate.now())
                && request.status() != ru.mirea.appliancerepair.backend.domain.RequestStatus.COMPLETED
                && request.status() != ru.mirea.appliancerepair.backend.domain.RequestStatus.CANCELLED;
        entity.setRiskLevel(riskCalculator.calculate(request.status(), request.priority(), request.urgent(), overdue));
        entity.setUpdatedBy(actor);
    }

    private void applyBulk(RepairRequest entity, BulkRequest request, UserAccount actor) {
        switch (request.action()) {
            case ARCHIVE -> archiveInternal(entity, actor);
            case RESTORE -> {
                entity.setArchived(false);
                entity.setArchivedAt(null);
                entity.setArchivedBy(null);
                entity.setUpdatedBy(actor);
            }
            case SET_STATUS -> {
                entity.setStatus(request.status());
                boolean overdue = entity.getPlannedDate() != null && entity.getPlannedDate().isBefore(LocalDate.now());
                entity.setRiskLevel(riskCalculator.calculate(entity.getStatus(), entity.getPriority(), entity.isUrgent(), overdue));
                entity.setUpdatedBy(actor);
            }
            case ADD_TAG -> {
                Set<String> tags = new LinkedHashSet<>(entity.getTags());
                tags.add(normalizeTag(request.tag()));
                entity.setTags(tags);
                entity.setUpdatedBy(actor);
            }
            case REMOVE_TAG -> {
                Set<String> tags = new LinkedHashSet<>(entity.getTags());
                tags.remove(normalizeTag(request.tag()));
                entity.setTags(tags);
                entity.setUpdatedBy(actor);
            }
        }
    }

    private static void archiveInternal(RepairRequest entity, UserAccount actor) {
        entity.setArchived(true);
        entity.setArchivedAt(LocalDateTime.now());
        entity.setArchivedBy(actor);
        entity.setUpdatedBy(actor);
    }

    private RepairRequest require(UUID id) {
        return requests.findById(id).orElseThrow(() -> new NotFoundException("Заявка не найдена"));
    }

    private Customer resolveCustomer(Long id) {
        return customers.findById(id).orElseThrow(() -> new BadRequestException("Клиент не найден"));
    }

    private ApplianceType resolveType(UUID id, String typeName) {
        ApplianceType type = id == null
                ? applianceTypes.findByNameIgnoreCase(normalizeRequired(typeName)).orElse(null)
                : applianceTypes.findById(id).orElse(null);
        if (type == null) throw new BadRequestException("Выберите существующий тип техники из справочника");
        if (!type.isActive()) throw new BadRequestException("Нельзя использовать отключённый тип техники");
        return type;
    }

    private void validateExternalId(String externalId, UUID currentId) {
        String value = normalizeNullable(externalId);
        if (value == null) return;
        boolean exists = currentId == null ? requests.existsByExternalIdIgnoreCase(value)
                : requests.existsByExternalIdIgnoreCaseAndIdNot(value, currentId);
        if (exists) throw new ConflictException("Внешний идентификатор уже используется");
    }

    private static void verifyVersion(Long requested, long actual) {
        if (requested == null) throw new BadRequestException("Для изменения требуется поле version");
        if (requested != actual) throw new ConflictException("Заявка уже изменена. Обновите данные и повторите операцию");
    }

    private static Set<String> normalizeTags(Set<String> tags) {
        if (tags == null) return Set.of();
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String tag : tags) result.add(normalizeTag(tag));
        if (result.size() > 20) throw new BadRequestException("Допустимо не более 20 тегов");
        return result;
    }

    private static String normalizeTag(String value) {
        String normalized = normalizeRequired(value).toLowerCase(Locale.ROOT);
        if (normalized.length() > 50) throw new BadRequestException("Тег длиннее 50 символов");
        return normalized;
    }

    private static String normalizeRequired(String value) {
        if (value == null || value.isBlank()) throw new BadRequestException("Обязательное поле не заполнено");
        return value.trim().replaceAll("\\s+", " ");
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim().replaceAll("\\s+", " ");
    }

    private static String createNumber() {
        return "ЗР-" + LocalDate.now().toString().replace("-", "") + "-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase(Locale.ROOT);
    }
}

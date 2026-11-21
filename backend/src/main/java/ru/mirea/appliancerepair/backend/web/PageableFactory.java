package ru.mirea.appliancerepair.backend.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import ru.mirea.appliancerepair.backend.error.BadRequestException;

import java.util.Map;

@Component
public class PageableFactory {
    private static final Map<String, String> REQUEST_FIELDS = Map.of(
            "number", "requestNumber", "created", "createdAt", "updated", "updatedAt",
            "planned", "plannedDate", "cost", "estimatedCost", "priority", "priority",
            "status", "status", "risk", "riskLevel", "urgent", "urgent");
    private static final Map<String, String> USER_FIELDS = Map.of(
            "email", "email", "name", "displayName", "created", "createdAt", "updated", "updatedAt");
    private static final Map<String, String> AUDIT_FIELDS = Map.of(
            "occurred", "occurredAt", "actor", "actorEmail", "event", "eventType");

    public Pageable requests(int page, int size, String sort) { return create(page, size, sort, REQUEST_FIELDS, "createdAt"); }
    public Pageable users(int page, int size, String sort) { return create(page, size, sort, USER_FIELDS, "email"); }
    public Pageable audit(int page, int size, String sort) { return create(page, size, sort, AUDIT_FIELDS, "occurredAt"); }

    private Pageable create(int page, int size, String sort, Map<String, String> allowed, String defaultField) {
        if (page < 0) throw new BadRequestException("page не может быть отрицательным");
        if (size < 1 || size > 100) throw new BadRequestException("size должен быть от 1 до 100");
        String[] parts = sort == null || sort.isBlank() ? new String[]{defaultField, "desc"} : sort.split(",", 2);
        String field = allowed.get(parts[0]);
        if (field == null) throw new BadRequestException("Недопустимое поле сортировки");
        Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, field));
    }
}

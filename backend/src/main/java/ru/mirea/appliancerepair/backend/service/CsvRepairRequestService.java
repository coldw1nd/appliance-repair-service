package ru.mirea.appliancerepair.backend.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.mirea.appliancerepair.backend.domain.RepairRequest;
import ru.mirea.appliancerepair.backend.domain.UserAccount;
import ru.mirea.appliancerepair.backend.dto.ImportDtos.ImportMode;
import ru.mirea.appliancerepair.backend.dto.ImportDtos.ImportPreview;
import ru.mirea.appliancerepair.backend.dto.ImportDtos.ImportResult;
import ru.mirea.appliancerepair.backend.dto.ImportDtos.RowIssue;
import ru.mirea.appliancerepair.backend.dto.RepairRequestDtos.RepairRequestRequest;
import ru.mirea.appliancerepair.backend.filter.RepairRequestSpecifications;
import ru.mirea.appliancerepair.backend.filter.SmartFilterParser;
import ru.mirea.appliancerepair.backend.repository.ApplianceTypeRepository;
import ru.mirea.appliancerepair.backend.repository.CustomerRepository;
import ru.mirea.appliancerepair.backend.repository.RepairRequestRepository;
import ru.mirea.appliancerepair.backend.security.CurrentUserService;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class CsvRepairRequestService {
    private static final int MAX_ROWS = 5000;
    private static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
    private static final List<String> REQUIRED_HEADERS = List.of("customerId", "applianceType", "applianceBrand",
            "issueTitle", "issueDescription", "status", "priority", "estimatedCost");
    private static final String[] HEADERS = {"externalId", "customerId", "applianceType", "applianceBrand",
            "issueTitle", "issueDescription", "status", "priority", "estimatedCost", "plannedDate", "urgent", "tags"};

    private final RepairRequestRepository requests;
    private final ApplianceTypeRepository applianceTypes;
    private final CustomerRepository customers;
    private final RepairRequestService requestService;
    private final RepairRequestMapper mapper;
    private final CurrentUserService currentUser;
    private final SmartFilterParser filterParser;
    private final AuditService audit;

    public CsvRepairRequestService(RepairRequestRepository requests, ApplianceTypeRepository applianceTypes,
                                  CustomerRepository customers, RepairRequestService requestService,
                                  RepairRequestMapper mapper, CurrentUserService currentUser,
                                  SmartFilterParser filterParser, AuditService audit) {
        this.requests = requests;
        this.applianceTypes = applianceTypes;
        this.customers = customers;
        this.requestService = requestService;
        this.mapper = mapper;
        this.currentUser = currentUser;
        this.filterParser = filterParser;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public ImportPreview preview(MultipartFile file, ImportMode mode) {
        List<RowIssue> issues = new ArrayList<>();
        ParsedFile parsed = parse(file, issues);
        validateDuplicatesAndMode(parsed.rows(), mode, issues);
        int invalid = (int) issues.stream().map(RowIssue::row).distinct().count();
        return new ImportPreview(parsed.totalRows(), Math.max(0, parsed.totalRows() - invalid), invalid,
                List.copyOf(issues));
    }

    @Transactional
    public ImportResult apply(MultipartFile file, ImportMode mode) {
        List<RowIssue> issues = new ArrayList<>();
        ParsedFile parsed = parse(file, issues);
        validateDuplicatesAndMode(parsed.rows(), mode, issues);
        if (!issues.isEmpty()) {
            return new ImportResult(parsed.totalRows(), 0, 0, parsed.totalRows(), List.copyOf(issues));
        }

        UserAccount actor = currentUser.requireEntity();
        int created = 0;
        int updated = 0;
        for (ParsedRow row : parsed.rows()) {
            RepairRequest existing = row.request().externalId() == null || row.request().externalId().isBlank()
                    ? null : requests.findByExternalIdIgnoreCase(row.request().externalId()).orElse(null);
            requestService.upsertFromImport(existing, row.request(), actor);
            if (existing == null) created++; else updated++;
        }
        audit.record("REPAIR_REQUEST_CSV_IMPORTED", "REPAIR_REQUEST", null, "Выполнен импорт CSV-файла",
                java.util.Map.of("totalRows", parsed.totalRows(), "created", created, "updated", updated));
        return new ImportResult(parsed.totalRows(), created, updated, 0, List.of());
    }

    @Transactional(readOnly = true)
    public void export(String query, OutputStream output) throws IOException {
        Specification<RepairRequest> specification = RepairRequestSpecifications.from(filterParser.parse(query));
        var result = requests.findAll(specification, org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
        writer.write('\uFEFF');
        try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(HEADERS).get())) {
            for (RepairRequest request : result) {
                var dto = mapper.toResponse(request);
                printer.printRecord(dto.externalId(), dto.customerId(), dto.applianceType(), dto.applianceBrand(),
                        dto.issueTitle(), dto.issueDescription(), dto.status(), dto.priority(), dto.estimatedCost(),
                        dto.plannedDate(), dto.urgent(), String.join("|", dto.tags()));
            }
        }
    }

    private ParsedFile parse(MultipartFile file, List<RowIssue> issues) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("CSV-файл пуст");
        if (file.getSize() > MAX_FILE_BYTES) throw new IllegalArgumentException("Размер CSV не должен превышать 5 МБ");

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8).replace("\uFEFF", "");
            CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
                    .setIgnoreEmptyLines(true).setTrim(true).get();
            List<ParsedRow> parsed = new ArrayList<>();
            int totalRows = 0;
            try (Reader reader = new StringReader(content); CSVParser csv = CSVParser.parse(reader, format)) {
                if (csv.getHeaderMap() == null || !csv.getHeaderMap().keySet().containsAll(REQUIRED_HEADERS)) {
                    throw new IllegalArgumentException("В CSV отсутствуют обязательные заголовки. Используйте sample-import.csv");
                }
                for (CSVRecord record : csv) {
                    totalRows++;
                    if (totalRows > MAX_ROWS) {
                        issues.add(new RowIssue(record.getRecordNumber() + 1, "file", "Превышен лимит 5000 строк"));
                        totalRows = MAX_ROWS + 1;
                        break;
                    }
                    try {
                        RepairRequestRequest request = toRequest(record);
                        parsed.add(new ParsedRow(record.getRecordNumber() + 1, request));
                    } catch (RuntimeException ex) {
                        issues.add(new RowIssue(record.getRecordNumber() + 1, "row", safeMessage(ex)));
                    }
                }
            }
            return new ParsedFile(List.copyOf(parsed), totalRows);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Не удалось прочитать CSV-файл");
        }
    }

    private RepairRequestRequest toRequest(CSVRecord row) {
        String externalId = optional(row, "externalId");
        checkLength(externalId, 100, "externalId");
        Long customerId = Long.valueOf(required(row, "customerId"));
        if (!customers.existsById(customerId)) throw new IllegalArgumentException("Клиент с таким customerId не найден");

        String applianceType = required(row, "applianceType");
        checkLength(applianceType, 80, "applianceType");
        var type = applianceTypes.findByNameIgnoreCase(applianceType)
                .orElseThrow(() -> new IllegalArgumentException("Тип техники отсутствует в справочнике: " + applianceType));
        if (!type.isActive()) throw new IllegalArgumentException("Тип техники отключён: " + applianceType);

        String brand = required(row, "applianceBrand");
        String title = required(row, "issueTitle");
        String description = required(row, "issueDescription");
        checkLength(brand, 80, "applianceBrand");
        checkLength(title, 200, "issueTitle");
        checkLength(description, 1000, "issueDescription");

        var status = ru.mirea.appliancerepair.backend.domain.RequestStatus.valueOf(required(row, "status").toUpperCase(Locale.ROOT));
        var priority = ru.mirea.appliancerepair.backend.domain.RequestPriority.valueOf(required(row, "priority").toUpperCase(Locale.ROOT));
        BigDecimal cost = new BigDecimal(required(row, "estimatedCost"));
        if (cost.signum() < 0 || cost.compareTo(new BigDecimal("1000000")) > 0 || cost.scale() > 2)
            throw new IllegalArgumentException("estimatedCost должен быть от 0 до 1000000 и содержать не более 2 знаков после запятой");

        String plannedText = optional(row, "plannedDate");
        LocalDate planned = plannedText.isBlank() ? null : LocalDate.parse(plannedText);
        String urgentText = optional(row, "urgent").toLowerCase(Locale.ROOT);
        boolean urgent = switch (urgentText) {
            case "", "false", "0", "no" -> false;
            case "true", "1", "yes" -> true;
            default -> throw new IllegalArgumentException("urgent должен быть true или false");
        };
        if (urgent && priority != ru.mirea.appliancerepair.backend.domain.RequestPriority.HIGH
                && priority != ru.mirea.appliancerepair.backend.domain.RequestPriority.URGENT)
            throw new IllegalArgumentException("Для urgent=true требуется HIGH или URGENT");

        Set<String> tags = new HashSet<>();
        String tagsText = optional(row, "tags");
        if (!tagsText.isBlank()) {
            for (String rawTag : tagsText.split("\\|")) {
                String tag = rawTag.trim();
                checkLength(tag, 50, "tags");
                if (!tag.isBlank()) tags.add(tag);
            }
        }
        if (tags.size() > 20) throw new IllegalArgumentException("Допустимо не более 20 тегов");

        return new RepairRequestRequest(externalId, customerId, applianceType, type.getId(), brand, title, description,
                status, priority, cost, planned, urgent, tags, null);
    }

    private void validateDuplicatesAndMode(List<ParsedRow> rows, ImportMode mode, List<RowIssue> issues) {
        Set<String> seen = new HashSet<>();
        for (ParsedRow row : rows) {
            String externalId = row.request().externalId();
            if (externalId != null && !externalId.isBlank() && !seen.add(externalId.toLowerCase(Locale.ROOT))) {
                issues.add(new RowIssue(row.number(), "externalId", "Повтор внешнего идентификатора в файле"));
            }
            if (mode == ImportMode.CREATE_ONLY && externalId != null && !externalId.isBlank()
                    && requests.existsByExternalIdIgnoreCase(externalId)) {
                issues.add(new RowIssue(row.number(), "externalId", "Такая заявка уже существует"));
            }
            if (mode == ImportMode.UPSERT_BY_EXTERNAL_ID && (externalId == null || externalId.isBlank())) {
                issues.add(new RowIssue(row.number(), "externalId", "Для режима обновления внешний идентификатор обязателен"));
            }
        }
    }

    private static String optional(CSVRecord record, String name) {
        try { return record.isMapped(name) ? record.get(name).trim() : ""; }
        catch (IllegalArgumentException ex) { return ""; }
    }

    private static String required(CSVRecord record, String name) {
        String value = optional(record, name);
        if (value.isBlank()) throw new IllegalArgumentException("Поле " + name + " обязательно");
        return value;
    }

    private static void checkLength(String value, int max, String field) {
        if (value != null && value.length() > max) throw new IllegalArgumentException("Поле " + field + " длиннее " + max + " символов");
    }

    private static String safeMessage(RuntimeException ex) {
        return ex.getMessage() == null ? "Строка не прошла проверку" : ex.getMessage();
    }

    private record ParsedRow(long number, RepairRequestRequest request) { }
    private record ParsedFile(List<ParsedRow> rows, int totalRows) { }
}

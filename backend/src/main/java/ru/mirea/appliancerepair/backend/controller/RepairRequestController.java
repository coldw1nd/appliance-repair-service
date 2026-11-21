package ru.mirea.appliancerepair.backend.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.mirea.appliancerepair.backend.dto.ImportDtos.ImportMode;
import ru.mirea.appliancerepair.backend.dto.ImportDtos.ImportPreview;
import ru.mirea.appliancerepair.backend.dto.ImportDtos.ImportResult;
import ru.mirea.appliancerepair.backend.dto.PageResponse;
import ru.mirea.appliancerepair.backend.dto.RepairRequestDtos.BulkRequest;
import ru.mirea.appliancerepair.backend.dto.RepairRequestDtos.BulkResult;
import ru.mirea.appliancerepair.backend.dto.RepairRequestDtos.RepairRequestRequest;
import ru.mirea.appliancerepair.backend.dto.RepairRequestDtos.RepairRequestResponse;
import ru.mirea.appliancerepair.backend.service.CsvRepairRequestService;
import ru.mirea.appliancerepair.backend.service.RepairRequestService;
import ru.mirea.appliancerepair.backend.web.PageableFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/repair-requests")
@SecurityRequirement(name = "bearerAuth")
public class RepairRequestController {
    private final RepairRequestService requests;
    private final CsvRepairRequestService csv;
    private final PageableFactory pageableFactory;

    public RepairRequestController(RepairRequestService requests, CsvRepairRequestService csv,
                                   PageableFactory pageableFactory) {
        this.requests = requests;
        this.csv = csv;
        this.pageableFactory = pageableFactory;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','REPAIRER','ADMIN')")
    public PageResponse<RepairRequestResponse> search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "created,desc") String sort) {
        return PageResponse.from(requests.search(q, pageableFactory.requests(page, size, sort)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VIEWER','REPAIRER','ADMIN')")
    public RepairRequestResponse get(@PathVariable UUID id) { return requests.get(id); }

    @PostMapping
    @PreAuthorize("hasAnyRole('REPAIRER','ADMIN')")
    public ResponseEntity<RepairRequestResponse> create(@Valid @RequestBody RepairRequestRequest request) {
        return ResponseEntity.status(201).body(requests.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('REPAIRER','ADMIN')")
    public RepairRequestResponse update(@PathVariable UUID id, @Valid @RequestBody RepairRequestRequest request) {
        return requests.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('REPAIRER','ADMIN')")
    public RepairRequestResponse archive(@PathVariable UUID id) { return requests.archive(id); }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('REPAIRER','ADMIN')")
    public RepairRequestResponse restore(@PathVariable UUID id) { return requests.restore(id); }

    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> permanentDelete(@PathVariable UUID id,
            @RequestHeader(name = "X-Confirm-Delete", required = false) String confirmation) {
        requests.permanentDelete(id, confirmation);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('REPAIRER','ADMIN')")
    public BulkResult bulk(@Valid @RequestBody BulkRequest request) { return requests.bulk(request); }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('REPAIRER','ADMIN')")
    public ImportPreview previewImport(@RequestPart("file") MultipartFile file,
                                       @RequestParam(defaultValue = "CREATE_ONLY") ImportMode mode) {
        return csv.preview(file, mode);
    }

    @PostMapping(value = "/import/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('REPAIRER','ADMIN')")
    public ImportResult applyImport(@RequestPart("file") MultipartFile file,
                                    @RequestParam(defaultValue = "CREATE_ONLY") ImportMode mode) {
        return csv.apply(file, mode);
    }

    @GetMapping(value = "/export", produces = "text/csv;charset=UTF-8")
    @PreAuthorize("hasAnyRole('REPAIRER','ADMIN')")
    public void export(@RequestParam(defaultValue = "") String q, HttpServletResponse response) throws IOException {
        String filename = "repair-requests-" + LocalDate.now() + ".csv";
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" +
                URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20"));
        csv.export(q, response.getOutputStream());
    }
}

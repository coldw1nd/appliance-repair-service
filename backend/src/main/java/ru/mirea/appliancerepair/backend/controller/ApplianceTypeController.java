package ru.mirea.appliancerepair.backend.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.appliancerepair.backend.dto.ApplianceTypeDtos.ApplianceTypeRequest;
import ru.mirea.appliancerepair.backend.dto.ApplianceTypeDtos.ApplianceTypeResponse;
import ru.mirea.appliancerepair.backend.service.ApplianceTypeService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/references", "/api/appliance-types"})
@SecurityRequirement(name = "bearerAuth")
public class ApplianceTypeController {
    private final ApplianceTypeService applianceTypes;
    public ApplianceTypeController(ApplianceTypeService applianceTypes) { this.applianceTypes = applianceTypes; }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','REPAIRER','ADMIN')")
    public List<ApplianceTypeResponse> list(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return applianceTypes.list(activeOnly);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApplianceTypeResponse> create(@Valid @RequestBody ApplianceTypeRequest request) {
        return ResponseEntity.status(201).body(applianceTypes.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApplianceTypeResponse update(@PathVariable UUID id, @Valid @RequestBody ApplianceTypeRequest request) {
        return applianceTypes.update(id, request);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ApplianceTypeResponse setActive(@PathVariable UUID id, @RequestParam boolean active,
                                           @RequestParam long version) {
        return applianceTypes.setActive(id, active, version);
    }
}

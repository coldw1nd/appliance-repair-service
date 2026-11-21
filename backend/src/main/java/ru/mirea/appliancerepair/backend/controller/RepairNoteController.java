package ru.mirea.appliancerepair.backend.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.appliancerepair.backend.dto.NoteDtos.NoteRequest;
import ru.mirea.appliancerepair.backend.dto.NoteDtos.NoteResponse;
import ru.mirea.appliancerepair.backend.service.NoteService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/repair-requests/{requestId}/notes")
@SecurityRequirement(name = "bearerAuth")
public class RepairNoteController {
    private final NoteService notes;
    public RepairNoteController(NoteService notes) { this.notes = notes; }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','REPAIRER','ADMIN')")
    public List<NoteResponse> list(@PathVariable UUID requestId) { return notes.list(requestId); }

    @PostMapping
    @PreAuthorize("hasAnyRole('REPAIRER','ADMIN')")
    public ResponseEntity<NoteResponse> create(@PathVariable UUID requestId, @Valid @RequestBody NoteRequest request) {
        return ResponseEntity.status(201).body(notes.create(requestId, request));
    }

    @PutMapping("/{noteId}")
    @PreAuthorize("hasAnyRole('REPAIRER','ADMIN')")
    public NoteResponse update(@PathVariable UUID requestId, @PathVariable UUID noteId,
                               @Valid @RequestBody NoteRequest request) {
        return notes.update(requestId, noteId, request);
    }

    @DeleteMapping("/{noteId}")
    @PreAuthorize("hasAnyRole('REPAIRER','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID requestId, @PathVariable UUID noteId) {
        notes.delete(requestId, noteId);
        return ResponseEntity.noContent().build();
    }
}

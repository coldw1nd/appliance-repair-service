package ru.mirea.appliancerepair.backend.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.appliancerepair.backend.domain.RepairNote;
import ru.mirea.appliancerepair.backend.domain.RepairRequest;
import ru.mirea.appliancerepair.backend.domain.Role;
import ru.mirea.appliancerepair.backend.domain.UserAccount;
import ru.mirea.appliancerepair.backend.dto.NoteDtos.NoteRequest;
import ru.mirea.appliancerepair.backend.dto.NoteDtos.NoteResponse;
import ru.mirea.appliancerepair.backend.error.ConflictException;
import ru.mirea.appliancerepair.backend.error.NotFoundException;
import ru.mirea.appliancerepair.backend.repository.RepairNoteRepository;
import ru.mirea.appliancerepair.backend.repository.RepairRequestRepository;
import ru.mirea.appliancerepair.backend.security.CurrentUserService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NoteService {
    private final RepairNoteRepository notes;
    private final RepairRequestRepository requests;
    private final CurrentUserService currentUser;
    private final AuditService audit;

    public NoteService(RepairNoteRepository notes, RepairRequestRepository requests,
                       CurrentUserService currentUser, AuditService audit) {
        this.notes = notes;
        this.requests = requests;
        this.currentUser = currentUser;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> list(UUID requestId) {
        requireRequest(requestId);
        return notes.findByRepairRequestIdOrderByPinnedDescCreatedAtDesc(requestId).stream().map(NoteService::toResponse).toList();
    }

    @Transactional
    public NoteResponse create(UUID requestId, NoteRequest request) {
        RepairRequest repairRequest = requireRequest(requestId);
        UserAccount actor = currentUser.requireEntity();
        RepairNote note = new RepairNote(UUID.randomUUID(), repairRequest, actor, request.body().trim(), request.pinned());
        notes.save(note);
        audit.record("NOTE_CREATED", "REPAIR_NOTE", note.getId(), "Добавлен комментарий к заявке " + repairRequest.getRequestNumber(), Map.of("requestId", requestId));
        return toResponse(note);
    }

    @Transactional
    public NoteResponse update(UUID requestId, UUID noteId, NoteRequest request) {
        RepairNote note = requireNote(requestId, noteId);
        UserAccount actor = currentUser.requireEntity();
        assertOwnerOrAdmin(note, actor);
        if (request.version() == null || request.version() != note.getVersion())
            throw new ConflictException("Комментарий уже изменён. Обновите данные");
        note.setBody(request.body().trim());
        note.setPinned(request.pinned());
        audit.record("NOTE_UPDATED", "REPAIR_NOTE", noteId, "Изменён комментарий к заявке", Map.of("requestId", requestId));
        return toResponse(note);
    }

    @Transactional
    public void delete(UUID requestId, UUID noteId) {
        RepairNote note = requireNote(requestId, noteId);
        UserAccount actor = currentUser.requireEntity();
        assertOwnerOrAdmin(note, actor);
        notes.delete(note);
        audit.record("NOTE_DELETED", "REPAIR_NOTE", noteId, "Удалён комментарий к заявке", Map.of("requestId", requestId));
    }

    private RepairRequest requireRequest(UUID id) {
        return requests.findById(id).orElseThrow(() -> new NotFoundException("Заявка не найдена"));
    }

    private RepairNote requireNote(UUID requestId, UUID noteId) {
        RepairNote note = notes.findById(noteId).orElseThrow(() -> new NotFoundException("Комментарий не найден"));
        if (!note.getRepairRequest().getId().equals(requestId)) throw new NotFoundException("Комментарий не найден");
        return note;
    }

    private static void assertOwnerOrAdmin(RepairNote note, UserAccount actor) {
        if (!note.getAuthor().getId().equals(actor.getId()) && !actor.getRoles().contains(Role.ADMIN))
            throw new AccessDeniedException("Only note author or administrator can modify it");
    }

    private static NoteResponse toResponse(RepairNote note) {
        return new NoteResponse(note.getId(), note.getRepairRequest().getId(), note.getAuthor().getId(),
                note.getAuthor().getDisplayName(), note.getBody(), note.isPinned(), note.getCreatedAt(), note.getUpdatedAt(), note.getVersion());
    }
}

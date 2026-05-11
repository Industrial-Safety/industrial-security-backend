package com.industrial.safety.chat_service.controller;

import com.industrial.safety.chat_service.dto.announcement.AnnouncementRequest;
import com.industrial.safety.chat_service.dto.announcement.AnnouncementResponse;
import com.industrial.safety.chat_service.service.AnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    // Visible para todos los no-alumnos (INSTRUCTOR, ADMIN, SOPORTE)
    @GetMapping
    public List<AnnouncementResponse> getAll() {
        return announcementService.getAll();
    }

    // Solo admins e instructores pueden publicar
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnnouncementResponse create(@Valid @RequestBody AnnouncementRequest request) {
        return announcementService.create(request);
    }

    // Solo admin puede eliminar
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        announcementService.delete(id);
    }
}

package com.CLMTZ.Backend.controller.general;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.CLMTZ.Backend.dto.general.NotificationDTO;
import com.CLMTZ.Backend.service.general.INotificationService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/general/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final INotificationService service;

    @GetMapping
    public ResponseEntity<List<NotificationDTO>> findAll() { return ResponseEntity.ok(service.findAll()); }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationDTO> findById(@PathVariable("id") Integer id) { return ResponseEntity.ok(service.findById(id)); }

    @PostMapping
    public ResponseEntity<NotificationDTO> save(@RequestBody NotificationDTO dto) { return new ResponseEntity<>(service.save(dto), HttpStatus.CREATED); }

    @PutMapping("/{id}")
    public ResponseEntity<NotificationDTO> update(@PathVariable("id") Integer id, @RequestBody NotificationDTO dto) { return ResponseEntity.ok(service.update(id, dto)); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) { service.deleteById(id); return ResponseEntity.noContent().build(); }
}

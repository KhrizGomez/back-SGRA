package com.CLMTZ.Backend.controller.general;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.CLMTZ.Backend.dto.general.NotificationChannelsDTO;
import com.CLMTZ.Backend.service.general.INotificationChannelsService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/general/notification-channels")
@RequiredArgsConstructor
public class NotificationChannelsController {

    private final INotificationChannelsService service;

    @GetMapping
    public ResponseEntity<List<NotificationChannelsDTO>> findAll() { return ResponseEntity.ok(service.findAll()); }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationChannelsDTO> findById(@PathVariable("id") Integer id) { return ResponseEntity.ok(service.findById(id)); }

    @PostMapping
    public ResponseEntity<NotificationChannelsDTO> save(@RequestBody NotificationChannelsDTO dto) { return new ResponseEntity<>(service.save(dto), HttpStatus.CREATED); }

    @PutMapping("/{id}")
    public ResponseEntity<NotificationChannelsDTO> update(@PathVariable("id") Integer id, @RequestBody NotificationChannelsDTO dto) { return ResponseEntity.ok(service.update(id, dto)); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) { service.deleteById(id); return ResponseEntity.noContent().build(); }
}

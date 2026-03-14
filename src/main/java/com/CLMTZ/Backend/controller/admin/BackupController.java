package com.CLMTZ.Backend.controller.admin;

import com.CLMTZ.Backend.dto.admin.BackupHistoryItemDTO;
import com.CLMTZ.Backend.dto.admin.BackupResultDTO;
import com.CLMTZ.Backend.dto.admin.BackupScheduleEntryDTO;
import com.CLMTZ.Backend.service.admin.IBackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/backup")
@RequiredArgsConstructor
public class BackupController {

    private final IBackupService backupService;

    // ─── Ejecución manual ──────────────────────────────────────────────────────

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate() {
        String version = backupService.validatePgDump();
        return ResponseEntity.ok(Map.of(
                "available", version != null,
                "version",   version != null ? version : "No encontrado"
        ));
    }

    @PostMapping("/trigger")
    public ResponseEntity<BackupResultDTO> trigger() {
        return ResponseEntity.ok(backupService.triggerManualBackup());
    }

    @GetMapping("/history")
    public ResponseEntity<List<BackupHistoryItemDTO>> history() {
        return ResponseEntity.ok(backupService.listBackups());
    }

    @DeleteMapping("/history/{fileName}")
    public ResponseEntity<Void> deleteBackup(@PathVariable String fileName) {
        try {
            backupService.deleteBackup(fileName);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/restore/{fileName}")
    public ResponseEntity<BackupResultDTO> restore(@PathVariable String fileName) {
        return ResponseEntity.ok(backupService.restoreBackup(fileName));
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Map<String, Object>> download(@PathVariable String fileName) {
        try {
            return ResponseEntity.ok(Map.of("url", backupService.getDownloadUrl(fileName)));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ─── Programaciones automáticas ────────────────────────────────────────────

    @GetMapping("/schedules")
    public ResponseEntity<List<BackupScheduleEntryDTO>> listSchedules() {
        return ResponseEntity.ok(backupService.listSchedules());
    }

    @PostMapping("/schedules")
    public ResponseEntity<BackupScheduleEntryDTO> createSchedule(@RequestBody BackupScheduleEntryDTO dto) {
        return ResponseEntity.ok(backupService.createSchedule(dto));
    }

    @PutMapping("/schedules/{id}")
    public ResponseEntity<BackupScheduleEntryDTO> updateSchedule(
            @PathVariable Integer id, @RequestBody BackupScheduleEntryDTO dto) {
        return ResponseEntity.ok(backupService.updateSchedule(id, dto));
    }

    @DeleteMapping("/schedules/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Integer id) {
        backupService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }
}

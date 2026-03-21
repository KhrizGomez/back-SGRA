package com.CLMTZ.Backend.controller.admin;

import com.CLMTZ.Backend.dto.admin.BackupBrowseDTO;
import com.CLMTZ.Backend.dto.admin.BackupHistoryItemDTO;
import com.CLMTZ.Backend.dto.admin.BackupLocalConfigDTO;
import com.CLMTZ.Backend.dto.admin.BackupResultDTO;
import com.CLMTZ.Backend.dto.admin.BackupScheduleEntryDTO;
import com.CLMTZ.Backend.service.admin.IBackupService;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
        return ResponseEntity.ok(backupService.restoreBackup(fileName, false));
    }

    @PostMapping("/restore-new-db/{fileName}")
    public ResponseEntity<BackupResultDTO> restoreToNewDatabase(@PathVariable String fileName) {
        return ResponseEntity.ok(backupService.restoreBackupToNewDatabase(fileName,false));
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Map<String, Object>> download(@PathVariable String fileName) {
        try {
            return ResponseEntity.ok(Map.of("url", backupService.getDownloadUrl(fileName)));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** Descarga el contenido del backup directamente desde Azure, evitando CORS en el browser. */
    @GetMapping("/stream/{fileName}")
    public void stream(@PathVariable String fileName, HttpServletResponse response) throws Exception {
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + fileName + "\"");
        backupService.streamBackup(fileName, response.getOutputStream());
    }

    // ─── Configuración ruta local ───────────────────────────────────────────────

    @GetMapping("/local-config")
    public ResponseEntity<BackupLocalConfigDTO> getLocalConfig() {
        BackupLocalConfigDTO config = backupService.getLocalConfig();
        if (config == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(config);
    }

    @PostMapping("/local-config")
    public ResponseEntity<BackupLocalConfigDTO> saveLocalConfig(@RequestBody Map<String, String> body) {
        String ruta = body.get("ruta");
        if (ruta == null || ruta.isBlank()) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(backupService.saveLocalConfig(ruta.trim()));
    }

    @GetMapping("/browse")
    public ResponseEntity<BackupBrowseDTO> browse(@RequestParam(required = false) String path) {
        try {
            return ResponseEntity.ok(backupService.browseDirectory(path));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
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

    // ─── Verificación de base de datos ──────────────────────────────────────────

    @GetMapping("/db-check")
    public ResponseEntity<Map<String, Object>> checkDatabase() {
        boolean available = backupService.isDatabaseAvailable();
        return ResponseEntity.ok(Map.of("available", available));
    }

    // ─── Restore extremos ────────────────────────────────────────────

    @PostMapping("/restorebd-no-existent")
    public ResponseEntity<Boolean> restoreDropBd(@RequestParam String fileName){
        Boolean success = backupService.restoreDropBd(fileName);
        return ResponseEntity.ok(success);
    }
}

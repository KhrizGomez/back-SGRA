package com.CLMTZ.Backend.controller.admin;

import com.CLMTZ.Backend.dto.admin.BackupHistoryItemDTO;
import com.CLMTZ.Backend.dto.admin.BackupResultDTO;
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

    /** Verifica si pg_dump está disponible en el servidor y retorna su versión. */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate() {
        String version = backupService.validatePgDump();
        return ResponseEntity.ok(Map.of(
                "available", version != null,
                "version", version != null ? version : "No encontrado"
        ));
    }

    /** Ejecuta un respaldo manual de la base de datos. */
    @PostMapping("/trigger")
    public ResponseEntity<BackupResultDTO> trigger() {
        return ResponseEntity.ok(backupService.triggerManualBackup());
    }

    /** Lista los respaldos almacenados en Azure Blob Storage. */
    @GetMapping("/history")
    public ResponseEntity<List<BackupHistoryItemDTO>> history() {
        return ResponseEntity.ok(backupService.listBackups());
    }

    /** Genera una URL de descarga temporal (SAS, válida 2 horas) para un respaldo. */
    @GetMapping("/download/{fileName}")
    public ResponseEntity<Map<String, Object>> download(@PathVariable String fileName) {
        try {
            String url = backupService.getDownloadUrl(fileName);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

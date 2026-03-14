package com.CLMTZ.Backend.service.admin;

import com.CLMTZ.Backend.dto.admin.BackupHistoryItemDTO;
import com.CLMTZ.Backend.dto.admin.BackupResultDTO;
import com.CLMTZ.Backend.dto.admin.BackupScheduleEntryDTO;

import java.util.List;

public interface IBackupService {
    // Backup manual / ejecución
    BackupResultDTO triggerManualBackup();
    List<BackupHistoryItemDTO> listBackups();
    String validatePgDump();
    String getDownloadUrl(String fileName);
    void deleteBackup(String fileName);

    // Programaciones automáticas
    List<BackupScheduleEntryDTO> listSchedules();
    BackupScheduleEntryDTO createSchedule(BackupScheduleEntryDTO dto);
    BackupScheduleEntryDTO updateSchedule(Integer id, BackupScheduleEntryDTO dto);
    void deleteSchedule(Integer id);
}

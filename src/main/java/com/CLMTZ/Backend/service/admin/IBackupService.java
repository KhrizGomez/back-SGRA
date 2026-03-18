package com.CLMTZ.Backend.service.admin;

import com.CLMTZ.Backend.dto.admin.BackupBrowseDTO;
import com.CLMTZ.Backend.dto.admin.BackupHistoryItemDTO;
import com.CLMTZ.Backend.dto.admin.BackupLocalConfigDTO;
import com.CLMTZ.Backend.dto.admin.BackupResultDTO;
import com.CLMTZ.Backend.dto.admin.BackupScheduleEntryDTO;

import java.util.List;

public interface IBackupService {
    // Backup manual / ejecución
    BackupResultDTO triggerManualBackup();
    BackupResultDTO restoreBackup(String fileName, Boolean cero);
    BackupResultDTO restoreBackupToNewDatabase(String fileName,Boolean connect);
    List<BackupHistoryItemDTO> listBackups();
    String validatePgDump();
    String getDownloadUrl(String fileName);
    void streamBackup(String fileName, java.io.OutputStream out) throws Exception;
    void deleteBackup(String fileName);

    // Programaciones automáticas
    List<BackupScheduleEntryDTO> listSchedules();
    BackupScheduleEntryDTO createSchedule(BackupScheduleEntryDTO dto);
    BackupScheduleEntryDTO updateSchedule(Integer id, BackupScheduleEntryDTO dto);
    void deleteSchedule(Integer id);

    // Configuración ruta local
    BackupLocalConfigDTO getLocalConfig();
    BackupLocalConfigDTO saveLocalConfig(String ruta);
    BackupBrowseDTO browseDirectory(String path);

    // Restore de casos extremos
    Boolean restoreDropBd(String fileName); 
}

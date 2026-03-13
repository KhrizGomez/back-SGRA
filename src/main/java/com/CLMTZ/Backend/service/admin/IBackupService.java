package com.CLMTZ.Backend.service.admin;

import com.CLMTZ.Backend.dto.admin.BackupHistoryItemDTO;
import com.CLMTZ.Backend.dto.admin.BackupResultDTO;

import java.util.List;

public interface IBackupService {
    BackupResultDTO triggerManualBackup();
    List<BackupHistoryItemDTO> listBackups();
    String validatePgDump();
    String getDownloadUrl(String fileName);
}

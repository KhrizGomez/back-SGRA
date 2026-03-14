package com.CLMTZ.Backend.repository.admin;

import com.CLMTZ.Backend.model.admin.BackupConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IBackupConfigRepository extends JpaRepository<BackupConfig, Integer> {
}

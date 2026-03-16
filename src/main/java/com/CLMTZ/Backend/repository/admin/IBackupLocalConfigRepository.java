package com.CLMTZ.Backend.repository.admin;

import com.CLMTZ.Backend.model.admin.BackupLocalConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IBackupLocalConfigRepository extends JpaRepository<BackupLocalConfig, Integer> {
}

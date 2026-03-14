package com.CLMTZ.Backend.repository.admin;

import com.CLMTZ.Backend.model.admin.BackupScheduleEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IBackupScheduleEntryRepository extends JpaRepository<BackupScheduleEntry, Integer> {
    List<BackupScheduleEntry> findByHabilitadoTrue();
}

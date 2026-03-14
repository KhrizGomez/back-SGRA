package com.CLMTZ.Backend.service.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Gestiona las tareas de backup automático programadas dinámicamente.
 * Permite múltiples schedules independientes, cada uno identificado por su ID.
 */
@Component
public class BackupScheduler {

    private static final Logger log = LoggerFactory.getLogger(BackupScheduler.class);

    private final ThreadPoolTaskScheduler taskScheduler;
    private final Map<Integer, ScheduledFuture<?>> activeTasks = new ConcurrentHashMap<>();

    public BackupScheduler() {
        this.taskScheduler = new ThreadPoolTaskScheduler();
        this.taskScheduler.setPoolSize(4);
        this.taskScheduler.setThreadNamePrefix("sgra-backup-");
        this.taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        this.taskScheduler.initialize();
    }

    /**
     * Registra o reemplaza la tarea para el schedule con el ID dado.
     * Si habilitado=false, cancela la tarea si existía.
     */
    public synchronized void applyEntry(Integer id, boolean habilitado, String cron, Runnable tarea) {
        ScheduledFuture<?> existing = activeTasks.remove(id);
        if (existing != null) existing.cancel(false);

        if (habilitado) {
            activeTasks.put(id, taskScheduler.schedule(tarea, new CronTrigger(cron)));
            log.info("Schedule #{} programado con cron: {}", id, cron);
        } else {
            log.info("Schedule #{} desactivado.", id);
        }
    }

    /** Cancela y elimina la tarea del schedule con el ID dado. */
    public synchronized void removeEntry(Integer id) {
        ScheduledFuture<?> task = activeTasks.remove(id);
        if (task != null) {
            task.cancel(false);
            log.info("Schedule #{} eliminado.", id);
        }
    }

    public boolean isActive(Integer id) {
        ScheduledFuture<?> t = activeTasks.get(id);
        return t != null && !t.isCancelled() && !t.isDone();
    }

    public int activeCount() {
        return activeTasks.size();
    }
}

package com.CLMTZ.Backend.model.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tbconfigbackup", schema = "general")
public class BackupConfig {

    @Id
    @Column(name = "id")
    private Integer id = 1;

    @Column(name = "habilitado", nullable = false)
    private boolean habilitado = false;

    @Column(name = "expresion_cron", length = 50, nullable = false)
    private String expresionCron = "0 0 2 * * SUN";

    @Column(name = "fecha_ultima_ejecucion")
    private LocalDateTime fechaUltimaEjecucion;

    @Column(name = "resultado_ultima_ejecucion", length = 500)
    private String resultadoUltimaEjecucion;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;
}

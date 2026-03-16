package com.CLMTZ.Backend.model.admin;

import com.CLMTZ.Backend.model.general.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tbprogramacionrespaldo", schema = "general")
public class BackupScheduleEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idprogramacionrespaldo")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idusuario", foreignKey = @ForeignKey(name = "fk_programacionrespaldo_usuario"))
    private User usuario;

    @Column(name = "habilitado", nullable = false)
    private boolean habilitado = true;

    /** DIARIO, SEMANAL, MENSUAL */
    @Column(name = "frecuencia", length = 10, nullable = false)
    private String frecuencia = "DIARIO";

    /** Solo para SEMANAL: uno o varios días separados por coma ("MON,WED,FRI") */
    @Column(name = "dia_semana", length = 200)
    private String diaSemana;

    /** Solo para MENSUAL: uno o varios días separados por coma ("1,15,28") */
    @Column(name = "dia_mes", length = 200)
    private String diaMes;

    /** Solo para MENSUAL: meses separados por coma ("1,3,6,12") o "*" para todos */
    @Column(name = "meses", length = 200)
    private String meses = "*";

    @Column(name = "hora", nullable = false)
    private int hora = 2;

    @Column(name = "minuto", nullable = false)
    private int minuto = 0;

    @Column(name = "fecha_ultima_ejecucion")
    private LocalDateTime fechaUltimaEjecucion;

    @Column(name = "resultado_ultima_ejecucion", length = 500)
    private String resultadoUltimaEjecucion;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;
}

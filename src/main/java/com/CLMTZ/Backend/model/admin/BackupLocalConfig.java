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
@Table(name = "tbconfigrespaldolocal", schema = "general")
public class BackupLocalConfig {

    @Id
    @Column(name = "idconfigrespaldolocal")
    private Integer id = 1;

    @Column(name = "ruta", length = 500, nullable = false)
    private String ruta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idusuario")
    private User usuario;

    @Column(name = "fecha_configuracion")
    private LocalDateTime fechaConfiguracion;
}

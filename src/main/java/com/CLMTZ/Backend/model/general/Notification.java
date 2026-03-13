package com.CLMTZ.Backend.model.general;

import com.CLMTZ.Backend.model.reinforcement.ScheduledReinforcement;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "tbnotificaciones", schema = "general")
public class Notification {
    @Id
    @Column(name = "idnotificacion")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idusuario", nullable = false, foreignKey = @ForeignKey(name = "fk_notificacion_usuario"))
    private User userId;

    @Column(name = "titulo", length = 100, nullable = false)
    private String title;

    @Column(name = "mensaje", nullable = false, columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "fechaenvio", nullable = false, columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private LocalDateTime dateSent = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idrefuerzoprogramado", nullable = true,
            foreignKey = @ForeignKey(name = "fk_notificacion_refuerzoprogramado"))
    private ScheduledReinforcement scheduledReinforcement;
}
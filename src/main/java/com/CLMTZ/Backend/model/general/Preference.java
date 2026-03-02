package com.CLMTZ.Backend.model.general;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "tbpreferencias", schema = "general")
public class Preference {
    @Id
    @Column(name = "idpreferencia")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer preferenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idusuario", nullable = false, foreignKey = @ForeignKey(name = "fk_preferencias_usuario"))
    private User userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idcanalnotificacion", nullable = false, foreignKey = @ForeignKey(name = "fk_preferencias_canal"))
    private NotificationChannels notificationChannelId;

    @Column(name = "anticipacionrecordatorio", nullable = false)
    private Integer reminderAdvance;
}
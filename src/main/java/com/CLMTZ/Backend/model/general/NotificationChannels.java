package com.CLMTZ.Backend.model.general;

import java.util.List;

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
@Table(name = "tbcanalesnotificaciones", schema = "general")
public class NotificationChannels {
    @Id
    @Column(name = "idcanalnotificacion")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer notificationChannelId;

    @Column(name = "nombrecanal",length = 10,nullable = false, unique = true)
    private String nameChannel;

    @Column(name = "estado", nullable = false, columnDefinition = "boolean default true")
    private Boolean state = true;

    @OneToMany(mappedBy = "notificationChannelId", fetch = FetchType.LAZY)
    private List<Preference> preferences;
}

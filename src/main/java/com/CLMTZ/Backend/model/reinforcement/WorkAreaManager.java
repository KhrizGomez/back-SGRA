package com.CLMTZ.Backend.model.reinforcement;

import com.CLMTZ.Backend.model.academic.AcademicArea;
import com.CLMTZ.Backend.model.general.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "tbgestorareastrabajos", schema = "reforzamiento")
public class WorkAreaManager {
    @Id
    @Column(name = "idgestorareatrabajo")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer workAreaManagerId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idusuario", foreignKey = @ForeignKey(name = "fk_gestorareatrabajo_usuario"))
    private User userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idareaacademica", foreignKey = @ForeignKey(name = "fk_gestorareatrabajo_areaacademica"))
    private AcademicArea areaAcademicId;

    @Column(name = "planta", length = 50, nullable = false)
    private String plant;

    @Column(name = "estado", nullable = false, columnDefinition = "boolean default true")
    private Boolean state = true;
}

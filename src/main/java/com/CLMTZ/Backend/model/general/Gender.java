package com.CLMTZ.Backend.model.general;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "tbgeneros", schema = "general")
public class Gender {
    @Id
    @Column(name = "idgenero")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer genderId;

    @Column(name = "nombregenero" ,length = 15, nullable = false)
    private String genderName;

    @Column(name = "estado", nullable = false,columnDefinition = "boolean default true")
    private Boolean state = true;

    @OneToMany(mappedBy = "idGender", fetch = FetchType.LAZY)
    private List<User> users;
}


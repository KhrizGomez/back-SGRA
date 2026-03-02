package com.CLMTZ.Backend.dto.academic;

import lombok.Data;

@Data
public class SyllabiLoadDTO {
    private String carreraTexto;
    private String asignaturaTexto;
    private Integer unidad;
    private String nombreTema;
    
    public String getNameSyllabi() {
        return this.nombreTema;
    }
}

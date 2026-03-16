package com.CLMTZ.Backend.repository.security.custom;

import com.CLMTZ.Backend.dto.security.InstitutionLogoDTO;

public interface IInstitutionLogoRepository {

    /**
     * Obtiene el logo activo (URL desencriptada) y el nombre de la institución.
     */
    InstitutionLogoDTO getActiveLogoByInstitutionId(Integer institutionId);
}

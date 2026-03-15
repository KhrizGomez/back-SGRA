package com.CLMTZ.Backend.service.security;

import com.CLMTZ.Backend.dto.security.InstitutionLogoDTO;

public interface IInstitutionLogoService {

    InstitutionLogoDTO getLogoByInstitutionId(Integer institutionId);

    /**
     * Obtiene el logo de la institución del usuario actualmente autenticado.
     */
    InstitutionLogoDTO getLogoForCurrentUser();
}

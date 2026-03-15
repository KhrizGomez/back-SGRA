package com.CLMTZ.Backend.repository.security.custom;

import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;

public interface IInstitutionLogoCustomRepository {
    SpResponseDTO assignLogoInstitution (String jsonLogoInstitution);
    SpResponseDTO updateLogoInstitution (String jsonLogoInstitution);
}

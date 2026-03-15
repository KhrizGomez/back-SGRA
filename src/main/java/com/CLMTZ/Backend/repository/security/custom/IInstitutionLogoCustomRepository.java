package com.CLMTZ.Backend.repository.security.custom;

import java.util.List;

import com.CLMTZ.Backend.dto.general.InstitutionLogoResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;

public interface IInstitutionLogoCustomRepository {
    List<InstitutionLogoResponseDTO> listInstitutionLogo();
    SpResponseDTO assignLogoInstitution (String jsonLogoInstitution);
    SpResponseDTO updateLogoInstitution (String jsonLogoInstitution);
}

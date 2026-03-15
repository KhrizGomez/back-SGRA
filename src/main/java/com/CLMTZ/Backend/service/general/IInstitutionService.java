package com.CLMTZ.Backend.service.general;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.CLMTZ.Backend.dto.general.InstitutionCUDDTO;
import com.CLMTZ.Backend.dto.general.InstitutionLogoResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;

public interface IInstitutionService {
    List<InstitutionLogoResponseDTO> listInstitutionLogo();
    SpResponseDTO assignLogoInstitution(InstitutionCUDDTO institution,MultipartFile file);
    SpResponseDTO updateLogoInstitution(InstitutionCUDDTO institution,MultipartFile file);
}

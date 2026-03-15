package com.CLMTZ.Backend.service.general.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.CLMTZ.Backend.dto.general.InstitutionCUDDTO;
import com.CLMTZ.Backend.dto.general.InstitutionLogoResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.repository.security.custom.IInstitutionLogoCustomRepository;
import com.CLMTZ.Backend.service.external.IStorageService;
import com.CLMTZ.Backend.service.general.IInstitutionService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InstitutionServiceImpl implements IInstitutionService {

    private final IStorageService storageService;
    private final IInstitutionLogoCustomRepository institutionLogoCustomRepo;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<InstitutionLogoResponseDTO> listInstitutionLogo() {
        try {
            return institutionLogoCustomRepo.listInstitutionLogo();
        } catch (Exception e) {
            throw new RuntimeException("Error al listar los logos de instituciones: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public SpResponseDTO assignLogoInstitution(InstitutionCUDDTO institution,MultipartFile file){

        try {
            if(file == null || file.isEmpty()) return new SpResponseDTO("Logo no procesado", false);
        else {
            String fileUrl =  storageService.uploadFiles(file);
            institution.setLurllogo(fileUrl);

            String jsonInstitution = objectMapper.writeValueAsString(institution);

            return institutionLogoCustomRepo.assignLogoInstitution(jsonInstitution);
        }
        } catch (Exception e) {
            return new SpResponseDTO("Error al registrar la asignacion del logo a la institucion", false);
        }
        
    }

    @Override
    @Transactional
    public SpResponseDTO updateLogoInstitution(InstitutionCUDDTO institution,MultipartFile file){

        try {
            if(file == null || file.isEmpty()) return new SpResponseDTO("Logo no procesado", false);
        else {
            String fileUrl =  storageService.uploadFiles(file);
            institution.setLurllogo(fileUrl);

            String jsonInstitution = objectMapper.writeValueAsString(institution);

            return institutionLogoCustomRepo.updateLogoInstitution(jsonInstitution);
        }
        } catch (Exception e) {
            return new SpResponseDTO("Error al actualizar el logo de la institucion", false);
        }
        
    }
}

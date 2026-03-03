package com.CLMTZ.Backend.service.security.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.CLMTZ.Backend.dto.security.Request.EmailSettingsRequestDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.repository.security.custom.IEmailSettingsCustomRepository;
import com.CLMTZ.Backend.service.security.IEmailSettingsService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailSettingsServiceImpl implements IEmailSettingsService {

    private final IEmailSettingsCustomRepository EmailSettingsCustomRepo;

    @Override
    @Transactional(readOnly = true)
    public List<EmailSettingsRequestDTO> listEmailSettings(String filter, Boolean state){
        try {
            return EmailSettingsCustomRepo.listEmailSettings(filter, state);
        } catch (Exception e) {
            throw new RuntimeException("Error al listar los correos: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public SpResponseDTO createEmail(EmailSettingsRequestDTO emailDTO){
        try {
            return EmailSettingsCustomRepo.createEmail(emailDTO.getIdusuario(), emailDTO.getPcorreoemisor(), emailDTO.getPaplicacionsontrasena());
        } catch (Exception e) {
            throw new RuntimeException("Error al guardar el nuevo correo: " + e.getMessage());
        }
    }
}

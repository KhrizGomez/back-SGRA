package com.CLMTZ.Backend.service.security;

import java.util.List;

import com.CLMTZ.Backend.dto.security.Request.EmailSettingsRequestDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;

public interface IEmailSettingsService {

    List<EmailSettingsRequestDTO> listEmailSettings(String filter, Boolean state);

    SpResponseDTO createEmail(EmailSettingsRequestDTO emailDTO);

    SpResponseDTO updateEmail(EmailSettingsRequestDTO emailDTO);

    EmailSettingsRequestDTO getEmailById(Integer idConfiguracionCorreo);
}

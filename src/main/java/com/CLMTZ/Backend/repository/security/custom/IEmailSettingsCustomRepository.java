package com.CLMTZ.Backend.repository.security.custom;

import java.util.List;

import com.CLMTZ.Backend.dto.security.Request.EmailSettingsRequestDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;

public interface IEmailSettingsCustomRepository {

    List<EmailSettingsRequestDTO> listEmailSettings(String filter, Boolean state);
    
    SpResponseDTO createEmail(Integer userid, String email, String passwordApp);
}

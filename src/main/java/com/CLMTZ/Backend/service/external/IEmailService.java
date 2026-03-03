package com.CLMTZ.Backend.service.external;

import com.CLMTZ.Backend.dto.security.Response.EmailSettingsResponseDTO;

public interface IEmailService {

    void sendEmail(EmailSettingsResponseDTO config, String addresse, String subject, String body);
    
}

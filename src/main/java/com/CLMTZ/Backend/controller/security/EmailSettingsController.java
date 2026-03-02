package com.CLMTZ.Backend.controller.security;

import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.CLMTZ.Backend.dto.security.Request.EmailSettingsRequestDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.UserListManagementResponseDTO;
import com.CLMTZ.Backend.service.security.IEmailSettingsService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/security/email-settings")
@RequiredArgsConstructor
public class EmailSettingsController {

    private final IEmailSettingsService emailSettingsSer;

    @GetMapping("/list-emails")
    public ResponseEntity<List<EmailSettingsRequestDTO>> listEmails(
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "state", required = false) Boolean state) {
        List<EmailSettingsRequestDTO> requestList = emailSettingsSer.listEmailSettings(filter,state);
        return ResponseEntity.ok(requestList);
    }

    @PostMapping("/create-email")
    public ResponseEntity<SpResponseDTO> createEmail(@RequestBody EmailSettingsRequestDTO emailDTO){
        SpResponseDTO dto = emailSettingsSer.createEmail(emailDTO);
        return ResponseEntity.ok(dto);
    }
}

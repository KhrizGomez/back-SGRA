package com.CLMTZ.Backend.controller.security;

import com.CLMTZ.Backend.dto.security.InstitutionLogoDTO;
import com.CLMTZ.Backend.service.security.IInstitutionLogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/security/institution-logo")
@RequiredArgsConstructor
public class InstitutionLogoController {

    private final IInstitutionLogoService logoService;

    @GetMapping("/{institutionId}")
    public ResponseEntity<?> getLogoByInstitution(@PathVariable Integer institutionId) {
        InstitutionLogoDTO logo = logoService.getLogoByInstitutionId(institutionId);
        if (logo == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(logo);
    }

    @GetMapping("/current")
    public ResponseEntity<?> getLogoForCurrentUser() {
        InstitutionLogoDTO logo = logoService.getLogoForCurrentUser();
        if (logo == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(logo);
    }
}

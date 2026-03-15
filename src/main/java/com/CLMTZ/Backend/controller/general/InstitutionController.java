package com.CLMTZ.Backend.controller.general;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.CLMTZ.Backend.dto.general.InstitutionCUDDTO;
import com.CLMTZ.Backend.dto.general.InstitutionLogoResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.service.general.IInstitutionService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/general/institutions")
@RequiredArgsConstructor
public class InstitutionController {

    private final IInstitutionService institutionSer;

    @GetMapping("/list-institution-logo")
    public ResponseEntity<List<InstitutionLogoResponseDTO>> listInstitutionLogo() {
        List<InstitutionLogoResponseDTO> list = institutionSer.listInstitutionLogo();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/assign-logo")
    public ResponseEntity<SpResponseDTO> assignLogoInstitution(@RequestBody InstitutionCUDDTO institution, @RequestPart MultipartFile file){
        SpResponseDTO responseDTO = institutionSer.assignLogoInstitution(institution, file);
        return ResponseEntity.ok(responseDTO);
    }

    @PutMapping("/update-logo")
    public ResponseEntity<SpResponseDTO> updateLogoInstitution(@RequestBody InstitutionCUDDTO institution, @RequestPart MultipartFile file){
        SpResponseDTO responseDTO = institutionSer.updateLogoInstitution(institution, file);
        return ResponseEntity.ok(responseDTO);
    }

}

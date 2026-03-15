package com.CLMTZ.Backend.controller.general;

import java.util.List;

import org.springframework.http.MediaType;
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

    @PostMapping(value = "/assign-logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SpResponseDTO> assignLogoInstitution(@RequestPart("institution") InstitutionCUDDTO institution, @RequestPart("file") MultipartFile file){
        SpResponseDTO responseDTO = institutionSer.assignLogoInstitution(institution, file);
        return ResponseEntity.ok(responseDTO);
    }

    @PutMapping(value = "/update-logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SpResponseDTO> updateLogoInstitution(@RequestPart("institution") InstitutionCUDDTO institution, @RequestPart("file") MultipartFile file){
        SpResponseDTO responseDTO = institutionSer.updateLogoInstitution(institution, file);
        return ResponseEntity.ok(responseDTO);
    }

}

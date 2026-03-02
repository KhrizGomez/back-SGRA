package com.CLMTZ.Backend.controller.security;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.CLMTZ.Backend.dto.security.Request.AccessRequestDTO;
import com.CLMTZ.Backend.service.security.IAccessService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/security/accesses")
@RequiredArgsConstructor
public class AccessController {
    
}

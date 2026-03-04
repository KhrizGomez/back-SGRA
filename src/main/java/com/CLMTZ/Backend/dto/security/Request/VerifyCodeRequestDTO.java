package com.CLMTZ.Backend.dto.security.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyCodeRequestDTO {
    private String email;
    private String code;
}


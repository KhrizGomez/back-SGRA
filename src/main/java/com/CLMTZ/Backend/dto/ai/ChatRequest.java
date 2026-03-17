package com.CLMTZ.Backend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    /** Módulo que origina el chat: "coordinacion", "estudiante", etc. */
    private String module;

    /** Mensaje del usuario */
    private String message;
}

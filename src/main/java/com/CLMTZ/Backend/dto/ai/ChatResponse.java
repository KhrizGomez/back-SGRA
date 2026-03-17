package com.CLMTZ.Backend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    private String response;
    private String module;
    private boolean success;
    private String error;
}

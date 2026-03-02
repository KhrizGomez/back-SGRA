package com.CLMTZ.Backend.dto.reinforcement.teacher;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for registering a virtual meeting link (RF13).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeacherVirtualLinkDTO {
    private String url;
}

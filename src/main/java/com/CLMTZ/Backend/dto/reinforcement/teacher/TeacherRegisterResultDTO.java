package com.CLMTZ.Backend.dto.reinforcement.teacher;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for registering session results (RF17).
 * File uploads (resources) are handled as multipart form data alongside this DTO.
 * duration format: "HH:mm" (e.g. "01:30")
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeacherRegisterResultDTO {
    private String observation;
    private String duration;
}

package com.CLMTZ.Backend.dto.reinforcement.teacher;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for marking attendance per participant (RF16).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeacherAttendanceMarkDTO {
    private Integer performedId;
    private List<AttendanceItemDTO> attendances;
}

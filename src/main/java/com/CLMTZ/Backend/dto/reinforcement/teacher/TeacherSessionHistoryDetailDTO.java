package com.CLMTZ.Backend.dto.reinforcement.teacher;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeacherSessionHistoryDetailDTO {
    private Integer scheduledId;
    private String subjectName;
    private String scheduledDate;
    private String modality;
    private String timeSlot;
    private String sessionType;
    private String statusName;
    private String estimatedDuration;

    // From tbrefuerzosrealizados
    private String observation;
    private String actualDuration;

    // Attendance summary
    private Integer totalParticipants;
    private Integer attendedCount;
    private Double attendancePercentage;

    // Per-student attendance
    private List<AttendanceStudentDTO> attendance;

    // Uploaded file resources
    private List<String> resources;

    // Virtual meeting link
    private String virtualLink;
}

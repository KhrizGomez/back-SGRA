package com.CLMTZ.Backend.dto.academic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnrollmentDetailDTO {
    private Integer enrollmentDetailId;
    private Boolean active;
    private Integer registrationId;
    private Integer subjectId;
    private Integer parallelId;
}

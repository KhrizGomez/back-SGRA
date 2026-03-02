package com.CLMTZ.Backend.dto.academic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClassDTO {
    private Integer idClass;
    private Boolean active;
    private Integer teacherId;
    private Integer subjectId;
    private Integer periodId;
    private Integer parallelId;
}

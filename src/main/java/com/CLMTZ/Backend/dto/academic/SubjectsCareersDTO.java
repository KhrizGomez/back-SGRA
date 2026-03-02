package com.CLMTZ.Backend.dto.academic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubjectsCareersDTO {
    private Integer subjectCareerId;
    private Boolean state;
    private Integer subjectId;
    private Integer careerId;
}

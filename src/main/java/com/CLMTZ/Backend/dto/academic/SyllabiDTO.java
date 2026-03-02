package com.CLMTZ.Backend.dto.academic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SyllabiDTO {
    private Integer syllabiId;
    private String nameSyllabi;
    private Integer unit;
    private Boolean state;
    private Integer subjectId;
    public void setCarreraTexto(String cellValue) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setCarreraTexto'");
    }

}

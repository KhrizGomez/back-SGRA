package com.CLMTZ.Backend.dto.academic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModalityDTO {
    private Integer idModality;
    private String modality;
    private Boolean state;
}

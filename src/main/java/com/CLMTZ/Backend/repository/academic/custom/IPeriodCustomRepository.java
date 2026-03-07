package com.CLMTZ.Backend.repository.academic.custom;

import com.CLMTZ.Backend.dto.academic.PeriodCUDDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;

public interface IPeriodCustomRepository {
    SpResponseDTO createPeriod(PeriodCUDDTO periodCUD);

    SpResponseDTO updatePeriod(PeriodCUDDTO periodCUD);
}

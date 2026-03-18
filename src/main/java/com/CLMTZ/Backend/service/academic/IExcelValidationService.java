package com.CLMTZ.Backend.service.academic;

import org.springframework.web.multipart.MultipartFile;

import com.CLMTZ.Backend.dto.ai.AIValidationResult;

public interface IExcelValidationService {

    AIValidationResult validateExcel(MultipartFile file, String loadType);
}

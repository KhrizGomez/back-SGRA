package com.CLMTZ.Backend.service.external;

import org.springframework.web.multipart.MultipartFile;

public interface IStorageService {  
    String uploadFiles(MultipartFile file);
}

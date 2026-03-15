package com.CLMTZ.Backend.service.external;

import org.springframework.web.multipart.MultipartFile;

public interface IStorageService {  
    String uploadFiles(MultipartFile file);
    byte[] downloadFile(String fileName);
    String getContentType(String fileName);
}

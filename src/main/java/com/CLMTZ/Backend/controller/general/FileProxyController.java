package com.CLMTZ.Backend.controller.general;

import com.CLMTZ.Backend.service.external.IStorageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/archivos-sgra")
@RequiredArgsConstructor
public class FileProxyController {

    private static final Logger log = LoggerFactory.getLogger(FileProxyController.class);

    private final IStorageService storageService;

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<byte[]> getFile(@PathVariable String fileName) {
        try {
            byte[] content = storageService.downloadFile(fileName);
            String contentType = storageService.getContentType(fileName);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(content.length);
            headers.setCacheControl("max-age=86400");

            return new ResponseEntity<>(content, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.warn("Archivo no encontrado en Azure Blob: {}", fileName);
            return ResponseEntity.notFound().build();
        }
    }
}

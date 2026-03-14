package com.CLMTZ.Backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BackupHistoryItemDTO {
    private String fileName;
    private String blobUrl;
    private Long fileSizeBytes;
    private String createdAt;
}

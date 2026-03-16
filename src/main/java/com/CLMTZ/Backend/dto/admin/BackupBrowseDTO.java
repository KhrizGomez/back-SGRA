package com.CLMTZ.Backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BackupBrowseDTO {
    /** Ruta absoluta del directorio actual (vacío = raíz del sistema de archivos). */
    private String currentPath;
    /** Ruta del directorio padre (null si ya estamos en la raíz). */
    private String parentPath;
    /** Nombres de los subdirectorios directos (no rutas completas, solo nombre). */
    private List<String> directories;
}

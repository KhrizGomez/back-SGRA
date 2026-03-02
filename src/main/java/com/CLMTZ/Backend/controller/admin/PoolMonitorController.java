package com.CLMTZ.Backend.controller.admin;

import com.CLMTZ.Backend.config.UserConnectionPool;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controlador para monitoreo de pools de conexiones dinámicas.
 * Solo accesible por administradores.
 */
@RestController
@RequestMapping("/api/admin/pools")
@RequiredArgsConstructor
public class PoolMonitorController {

    private final UserConnectionPool userConnectionPool;

    /**
     * Obtiene estadísticas de todos los pools de conexiones activos.
     */
    @GetMapping
    public ResponseEntity<Map<String, UserConnectionPool.PoolStats>> getPoolStats() {
        return ResponseEntity.ok(userConnectionPool.getPoolStats());
    }
}


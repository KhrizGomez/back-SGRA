package com.CLMTZ.Backend.repository.security.jpa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.CLMTZ.Backend.model.security.EmailSettings;

public interface IEmailSettingsRepository extends JpaRepository<EmailSettings, Integer> {

    /**
     * Obtiene la primera configuración de correo activa.
     * Se usa para envío interno de emails (credenciales, notificaciones, etc.)
     */
    Optional<EmailSettings> findFirstByStateTrue();
}

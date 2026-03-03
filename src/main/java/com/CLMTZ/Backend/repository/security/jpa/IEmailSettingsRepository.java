package com.CLMTZ.Backend.repository.security.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import com.CLMTZ.Backend.model.security.EmailSettings;

public interface IEmailSettingsRepository extends JpaRepository<EmailSettings, Integer> {

}

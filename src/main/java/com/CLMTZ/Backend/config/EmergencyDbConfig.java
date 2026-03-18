package com.CLMTZ.Backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariDataSource;

@Component
public class EmergencyDbConfig {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String adminUsername;

    @Value("${spring.datasource.password}")
    private String adminPassword;

    public JdbcTemplate emergencyDataSource(){

        String[] urlParts = datasourceUrl.split("\\?"); 
        String urlWithoutParams = urlParts[0];
        String sslParams = urlParts.length > 1 ? "?" + urlParts[1] : "";

        String baseUrl = urlWithoutParams.substring(0, urlWithoutParams.lastIndexOf("/")) + "/postgres" + sslParams;

        HikariDataSource hds = new HikariDataSource();
        hds.setJdbcUrl(baseUrl);
        hds.setUsername(adminUsername);
        hds.setPassword(adminPassword);

        return new JdbcTemplate(hds);
    }
}

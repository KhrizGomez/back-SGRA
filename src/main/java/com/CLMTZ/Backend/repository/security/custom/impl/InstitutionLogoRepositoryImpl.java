package com.CLMTZ.Backend.repository.security.custom.impl;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import com.CLMTZ.Backend.dto.security.InstitutionLogoDTO;
import com.CLMTZ.Backend.repository.security.custom.IInstitutionLogoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class InstitutionLogoRepositoryImpl implements IInstitutionLogoRepository {

    private static final Logger log = LoggerFactory.getLogger(InstitutionLogoRepositoryImpl.class);

    private final DynamicDataSourceService dynamicDataSourceService;

    public InstitutionLogoRepositoryImpl(DynamicDataSourceService dynamicDataSourceService) {
        this.dynamicDataSourceService = dynamicDataSourceService;
    }

    private NamedParameterJdbcTemplate getJdbcTemplate() {
        return dynamicDataSourceService.getDefaultJdbcTemplate();
    }

    @Override
    public InstitutionLogoDTO getActiveLogoByInstitutionId(Integer institutionId) {
        try {
            String sql = """
                SELECT
                    l.idinstitucion,
                    public.pgp_sym_decrypt(
                        public.dearmor(l.urllogo),
                        '0147852369'
                    ) AS urllogo,
                    i.nombreinstitucion
                FROM seguridad.tblogoinstituciones l
                JOIN general.tbinstituciones i ON i.idinstitucion = l.idinstitucion
                WHERE l.idinstitucion = :institutionId
                  AND l.estado = true
                ORDER BY l.idinstitucion
                LIMIT 1
                """;

            MapSqlParameterSource params = new MapSqlParameterSource("institutionId", institutionId);
            List<Map<String, Object>> rows = getJdbcTemplate().queryForList(sql, params);

            if (rows.isEmpty()) {
                return null;
            }

            Map<String, Object> row = rows.get(0);
            InstitutionLogoDTO dto = new InstitutionLogoDTO();
            dto.setInstitutionId((Integer) row.get("idinstitucion"));
            dto.setLogoUrl((String) row.get("urllogo"));
            dto.setInstitutionName((String) row.get("nombreinstitucion"));
            return dto;

        } catch (Exception e) {
            log.error("Error al obtener logo de institución {}: {}", institutionId, e.getMessage(), e);
            return null;
        }
    }
}

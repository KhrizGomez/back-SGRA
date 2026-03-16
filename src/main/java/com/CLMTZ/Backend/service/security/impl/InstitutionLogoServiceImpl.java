package com.CLMTZ.Backend.service.security.impl;

import com.CLMTZ.Backend.config.UserContextHolder;
import com.CLMTZ.Backend.dto.security.InstitutionLogoDTO;
import com.CLMTZ.Backend.dto.security.session.UserContext;
import com.CLMTZ.Backend.repository.security.custom.IInstitutionLogoRepository;
import com.CLMTZ.Backend.service.security.IInstitutionLogoService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InstitutionLogoServiceImpl implements IInstitutionLogoService {

    private static final Logger log = LoggerFactory.getLogger(InstitutionLogoServiceImpl.class);

    private final IInstitutionLogoRepository logoRepository;

    @Override
    public InstitutionLogoDTO getLogoByInstitutionId(Integer institutionId) {
        return logoRepository.getActiveLogoByInstitutionId(institutionId);
    }

    @Override
    public InstitutionLogoDTO getLogoForCurrentUser() {
        UserContext ctx = UserContextHolder.getContext();
        if (ctx == null) {
            log.warn("No hay contexto de usuario para obtener logo de institución");
            return null;
        }

        if (ctx.getInstitutionId() == null) {
            log.warn("Usuario {} no tiene institución asignada", ctx.getUserId());
            return null;
        }

        return logoRepository.getActiveLogoByInstitutionId(ctx.getInstitutionId());
    }
}

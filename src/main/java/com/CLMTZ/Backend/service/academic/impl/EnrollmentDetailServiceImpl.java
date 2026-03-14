package com.CLMTZ.Backend.service.academic.impl;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import com.CLMTZ.Backend.dto.academic.EnrollmentDetailDTO;
import com.CLMTZ.Backend.dto.academic.EnrollmentDetailLoadDTO;
import com.CLMTZ.Backend.model.academic.EnrollmentDetail;
import com.CLMTZ.Backend.repository.academic.*;
import com.CLMTZ.Backend.service.academic.IEnrollmentDetailService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EnrollmentDetailServiceImpl implements IEnrollmentDetailService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentDetailServiceImpl.class);

    private final IEnrollmentDetailRepository repository;
    private final IRegistrationsRepository registrationsRepository;
    private final ISubjectRepository subjectRepository;
    private final IParallelRepository parallelRepository;
    private final DynamicDataSourceService dynamicDataSourceService;

    // --- CRUD ---

    @Override
    public List<EnrollmentDetailDTO> findAll() {
        return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public EnrollmentDetailDTO findById(Integer id) {
        return repository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("EnrollmentDetail not found with id: " + id));
    }

    @Override
    public EnrollmentDetailDTO save(EnrollmentDetailDTO dto) {
        return toDTO(repository.save(toEntity(dto)));
    }

    @Override
    public EnrollmentDetailDTO update(Integer id, EnrollmentDetailDTO dto) {
        EnrollmentDetail entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("EnrollmentDetail not found with id: " + id));
        entity.setActive(dto.getActive());
        if (dto.getRegistrationId() != null) entity.setRegistrationId(registrationsRepository.findById(dto.getRegistrationId()).orElseThrow(() -> new RuntimeException("Registration not found")));
        if (dto.getSubjectId() != null) entity.setSubjectId(subjectRepository.findById(dto.getSubjectId()).orElseThrow(() -> new RuntimeException("Subject not found")));
        if (dto.getParallelId() != null) entity.setParallelId(parallelRepository.findById(dto.getParallelId()).orElseThrow(() -> new RuntimeException("Parallel not found")));
        return toDTO(repository.save(entity));
    }

    @Override
    public void deleteById(Integer id) {
        repository.deleteById(id);
    }

    // --- CARGA MASIVA ---

    @Override
    @Transactional
    public List<String> uploadEnrollmentDetails(List<EnrollmentDetailLoadDTO> registrationDTOs) {
        List<String> report = new ArrayList<>();

        if (registrationDTOs == null || registrationDTOs.isEmpty()) {
            report.add("ADVERTENCIA: No se encontraron registros de matrícula en el archivo. Verifique el formato del Excel.");
            return report;
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String jsonData = mapper.writeValueAsString(registrationDTOs);

            System.out.println("[UPLOAD-REGISTRATIONS] Enviando " + registrationDTOs.size() + " registros al SP como JSON");
            String resultadoSP = ejecutarCargaDetalleMatriculaSP(jsonData);
            report.add(resultadoSP);

        } catch (Exception e) {
            report.add("ERROR GENERAL: " + e.getMessage());
            e.printStackTrace();
        }
        // Línea de resumen comentada por solicitud del usuario
        // report.add(0, "RESUMEN: " + registrationDTOs.size() + " registros procesados → " + exitosos + " exitosos, " + errores + " con errores/advertencias.");

        return report;
    }

    // --- STORED PROCEDURE ---

    private String ejecutarCargaDetalleMatriculaSP(String jsonData) {
        try {
            String sql = "CALL academico.sp_in_carga_detalle_matricula(?, ?, ?)";
            JdbcTemplate jdbcTemplate = dynamicDataSourceService.getJdbcTemplate().getJdbcTemplate();

            return jdbcTemplate.execute(
                (Connection con) -> {
                    CallableStatement cs = con.prepareCall(sql);
                    cs.setObject(1, jsonData, Types.OTHER);
                    cs.registerOutParameter(2, Types.VARCHAR);
                    cs.registerOutParameter(3, Types.BOOLEAN);
                    return cs;
                },
                (CallableStatement cs) -> {
                    cs.execute();
                    String mensaje = cs.getString(2);
                    Boolean exito = cs.getBoolean(3);
                    log.info("sp_in_carga_detalle_matricula → exito={}, mensaje={}", exito, mensaje);
                    return Boolean.TRUE.equals(exito) ? "OK: " + mensaje : "FALLÓ SP: " + mensaje;
                }
            );

        } catch (Exception e) {
            String causaMsg = e.getCause() != null && e.getCause().getMessage() != null
                    ? e.getCause().getMessage() : e.getMessage();
            log.error("Error al ejecutar SP carga detalle matrícula: {}", causaMsg, e);
            return "FALLÓ SP: Error interno: " + causaMsg;
        }
    }

    // --- CONVERSORES DTO ---

    private EnrollmentDetailDTO toDTO(EnrollmentDetail entity) {
        EnrollmentDetailDTO dto = new EnrollmentDetailDTO();
        dto.setEnrollmentDetailId(entity.getEnrollmentDetailId());
        dto.setActive(entity.getActive());
        dto.setRegistrationId(entity.getRegistrationId() != null ? entity.getRegistrationId().getRegistrationId() : null);
        dto.setSubjectId(entity.getSubjectId() != null ? entity.getSubjectId().getIdSubject() : null);
        dto.setParallelId(entity.getParallelId() != null ? entity.getParallelId().getParallelId() : null);
        return dto;
    }

    private EnrollmentDetail toEntity(EnrollmentDetailDTO dto) {
        EnrollmentDetail entity = new EnrollmentDetail();
        entity.setActive(dto.getActive() != null ? dto.getActive() : true);
        if (dto.getRegistrationId() != null) entity.setRegistrationId(registrationsRepository.findById(dto.getRegistrationId()).orElseThrow(() -> new RuntimeException("Registration not found")));
        if (dto.getSubjectId() != null) entity.setSubjectId(subjectRepository.findById(dto.getSubjectId()).orElseThrow(() -> new RuntimeException("Subject not found")));
        if (dto.getParallelId() != null) entity.setParallelId(parallelRepository.findById(dto.getParallelId()).orElseThrow(() -> new RuntimeException("Parallel not found")));
        return entity;
    }
}

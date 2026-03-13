package com.CLMTZ.Backend.service.academic.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.CLMTZ.Backend.dto.academic.EnrollmentDetailDTO;
import com.CLMTZ.Backend.dto.academic.EnrollmentDetailLoadDTO;
import com.CLMTZ.Backend.model.academic.EnrollmentDetail;
import com.CLMTZ.Backend.repository.academic.*;
import com.CLMTZ.Backend.service.academic.IEnrollmentDetailService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EnrollmentDetailServiceImpl implements IEnrollmentDetailService {

    private final IEnrollmentDetailRepository repository;
    private final IRegistrationsRepository registrationsRepository;
    private final ISubjectRepository subjectRepository;
    private final IParallelRepository parallelRepository;

    @PersistenceContext
    private EntityManager entityManager;

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

        return report;
    }

    // --- STORED PROCEDURE ---

    private String ejecutarCargaDetalleMatriculaSP(String jsonData) {
        try {
            org.hibernate.Session session = entityManager.unwrap(org.hibernate.Session.class);
            Object[] result = session.doReturningWork(connection -> {
                try (java.sql.CallableStatement cs = connection.prepareCall(
                        "CALL academico.sp_in_carga_detalle_matricula(?, ?, ?)")) {
                    cs.setObject(1, jsonData, java.sql.Types.OTHER);
                    cs.registerOutParameter(2, java.sql.Types.VARCHAR);
                    cs.registerOutParameter(3, java.sql.Types.BOOLEAN);
                    cs.execute();
                    return new Object[]{cs.getString(2), cs.getBoolean(3)};
                }
            });

            String mensaje = (String) result[0];
            Boolean exito = (Boolean) result[1];
            return Boolean.TRUE.equals(exito) ? "OK: " + mensaje : "FALLÓ SP: " + mensaje;

        } catch (Exception e) {
            entityManager.clear();
            String errorMsg = e.getMessage() != null ? e.getMessage() : "";
            Throwable causa = e.getCause();
            String causaMsg = causa != null && causa.getMessage() != null ? causa.getMessage() : errorMsg;
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

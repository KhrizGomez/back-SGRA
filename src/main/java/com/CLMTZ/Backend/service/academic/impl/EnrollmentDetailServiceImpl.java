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
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
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

        for (EnrollmentDetailLoadDTO dto : registrationDTOs) {
            try {
                System.out.println("Procesando matrícula: " + dto.getIdentificador() + " - " + dto.getAsignatura());

                String resultadoSP = ejecutarCargaDetalleMatriculaSP(
                        dto.getIdentificador(), dto.getAsignatura(),
                        dto.getSemestre(), dto.getParalelo(), dto.getSexo());

                if (resultadoSP.startsWith("OK:")) {
                    String mensajeSP = resultadoSP.substring(3).trim();
                    report.add("Asignatura: " + dto.getAsignatura() + " → ID " + dto.getIdentificador() + ": " + mensajeSP);
                } else {
                    report.add("ID " + dto.getIdentificador() + " - " + dto.getAsignatura() + ": " + resultadoSP);
                }

            } catch (Exception e) {
                report.add("ID " + dto.getIdentificador() + " - " + dto.getAsignatura() + ": ERROR (" + e.getMessage() + ")");
                e.printStackTrace();
            }
        }

        long exitosos = report.stream().filter(r -> r.startsWith("Asignatura:")).count();
        long errores = report.size() - exitosos;
        // Línea de resumen comentada por solicitud del usuario
        // report.add(0, "RESUMEN: " + registrationDTOs.size() + " registros procesados → " + exitosos + " exitosos, " + errores + " con errores/advertencias.");

        return report;
    }

    // --- STORED PROCEDURE ---

    private String ejecutarCargaDetalleMatriculaSP(String identificador, String asignatura,
            Integer semestre, String paralelo, String sexo) {

        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("academico.sp_in_carga_detalle_matricula");
        query.registerStoredProcedureParameter("p_identificador_est", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_nombre_asignatura", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_semestre", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_paralelo", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_sexo", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_mensaje", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("p_exito", Boolean.class, ParameterMode.OUT);

        query.setParameter("p_identificador_est", identificador);
        query.setParameter("p_nombre_asignatura", asignatura);
        query.setParameter("p_semestre", semestre);
        query.setParameter("p_paralelo", paralelo);
        query.setParameter("p_sexo", sexo);
        query.execute();

        String mensaje = (String) query.getOutputParameterValue("p_mensaje");
        Boolean exito = (Boolean) query.getOutputParameterValue("p_exito");
        return Boolean.TRUE.equals(exito) ? "OK: " + mensaje : "FALLÓ SP: " + mensaje;
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

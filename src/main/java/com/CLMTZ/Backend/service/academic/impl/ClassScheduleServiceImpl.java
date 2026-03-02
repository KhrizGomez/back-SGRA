package com.CLMTZ.Backend.service.academic.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.CLMTZ.Backend.dto.academic.ClassScheduleDTO;
import com.CLMTZ.Backend.dto.academic.ClassScheduleDetailDTO;
import com.CLMTZ.Backend.dto.academic.ClassScheduleLoadDTO;
import com.CLMTZ.Backend.model.academic.ClassSchedule;
import com.CLMTZ.Backend.repository.academic.*;
import com.CLMTZ.Backend.service.academic.IClassScheduleService;

import lombok.RequiredArgsConstructor;
//import lombok.var;

@Service
@RequiredArgsConstructor
public class ClassScheduleServiceImpl implements IClassScheduleService {

    private final IClassScheduleRepository repository;
    private final ITimeSlotRepository timeSlotRepository;
    private final IClassRepository classRepository;
    private final IPeriodRepository periodRepository;
    private final ITeachingRepository teachingRepository;

    @Override
    public List<ClassScheduleDTO> findAll() {
        return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public ClassScheduleDTO findById(Integer id) {
        return repository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("ClassSchedule not found with id: " + id));
    }

    @Override
    public ClassScheduleDTO save(ClassScheduleDTO dto) {
        return toDTO(repository.save(toEntity(dto)));
    }

    @Override
    public ClassScheduleDTO update(Integer id, ClassScheduleDTO dto) {
        ClassSchedule entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ClassSchedule not found with id: " + id));
        entity.setDay(dto.getDay());
        entity.setActive(dto.getActive());
        if (dto.getTimeSlotId() != null)
            entity.setTimeSlotId(timeSlotRepository.findById(dto.getTimeSlotId())
                    .orElseThrow(() -> new RuntimeException("TimeSlot not found")));
        if (dto.getAssignedClassId() != null)
            entity.setAssignedClassId(classRepository.findById(dto.getAssignedClassId())
                    .orElseThrow(() -> new RuntimeException("Class not found")));
        if (dto.getPeriodId() != null)
            entity.setPeriodId(periodRepository.findById(dto.getPeriodId())
                    .orElseThrow(() -> new RuntimeException("Period not found")));
        return toDTO(repository.save(entity));
    }

    @Override
    public void deleteById(Integer id) {
        repository.deleteById(id);
    }

    private ClassScheduleDTO toDTO(ClassSchedule entity) {
        ClassScheduleDTO dto = new ClassScheduleDTO();
        dto.setIdClassSchedule(entity.getIdClassSchedule());
        dto.setDay(entity.getDay());
        dto.setActive(entity.getActive());
        dto.setTimeSlotId(entity.getTimeSlotId() != null ? entity.getTimeSlotId().getTimeSlotId() : null);
        dto.setAssignedClassId(entity.getAssignedClassId() != null ? entity.getAssignedClassId().getIdClass() : null);
        dto.setPeriodId(entity.getPeriodId() != null ? entity.getPeriodId().getPeriodId() : null);
        return dto;
    }

    private ClassSchedule toEntity(ClassScheduleDTO dto) {
        ClassSchedule entity = new ClassSchedule();
        entity.setDay(dto.getDay());
        entity.setActive(dto.getActive() != null ? dto.getActive() : true);
        if (dto.getTimeSlotId() != null)
            entity.setTimeSlotId(timeSlotRepository.findById(dto.getTimeSlotId())
                    .orElseThrow(() -> new RuntimeException("TimeSlot not found")));
        if (dto.getAssignedClassId() != null)
            entity.setAssignedClassId(classRepository.findById(dto.getAssignedClassId())
                    .orElseThrow(() -> new RuntimeException("Class not found")));
        if (dto.getPeriodId() != null)
            entity.setPeriodId(periodRepository.findById(dto.getPeriodId())
                    .orElseThrow(() -> new RuntimeException("Period not found")));
        return entity;
    }

    @Override
    public List<String> uploadClassSchedules(List<ClassScheduleLoadDTO> scheduleDTOs) {
        List<String> resultados = new ArrayList<>();
        if (scheduleDTOs == null || scheduleDTOs.isEmpty()) {
            resultados.add("No se encontraron filas para procesar.");
            return resultados;
        }
        for (int i = 0; i < scheduleDTOs.size(); i++) {
            int filaExcel = i + 2;
            ClassScheduleLoadDTO fila = scheduleDTOs.get(i);
            try {
                // 1. VALIDACIÓN PREVENTIVA (Evita NullPointerExceptions al hacer .trim)
                String cedula = fila.getCedulaDocente() != null ? fila.getCedulaDocente().trim() : "";
                String asignatura = fila.getNombreAsignatura() != null ? fila.getNombreAsignatura().trim() : "";
                String paralelo = fila.getNombreParalelo() != null ? fila.getNombreParalelo().trim() : "";
                String periodo = fila.getNombrePeriodo() != null ? fila.getNombrePeriodo().trim() : "";

                if (cedula.isEmpty() || asignatura.isEmpty() || paralelo.isEmpty() || periodo.isEmpty()) {
                    resultados.add("Fila " + filaExcel + ": ERROR (Faltan datos obligatorios de la clase)");
                    continue;
                }

                // 2. BUSCAR CLASE BASE
                var claseOpt = classRepository
                        .findByTeacherId_UserId_IdentificationAndSubjectId_SubjectIgnoreCaseAndParallelId_SectionIgnoreCaseAndPeriodId_PeriodIgnoreCase(
                                cedula, asignatura, paralelo, periodo);

                if (claseOpt.isEmpty()) {
                    resultados.add("Fila " + filaExcel
                            + ": ERROR (No existe clase asignada para ese docente/asignatura/paralelo/periodo)");
                    continue;
                }

                // 3. BUSCAR FRANJA HORARIA
                if (fila.getHoraInicio() == null || fila.getHoraFin() == null) {
                    resultados.add("Fila " + filaExcel + ": ERROR (Las horas de inicio y fin son obligatorias)");
                    continue;
                }

                var franjaOpt = timeSlotRepository.findByStartTimeAndEndTime(fila.getHoraInicio(), fila.getHoraFin());

                if (franjaOpt.isEmpty()) {
                    resultados.add("Fila " + filaExcel + ": ERROR (No existe franja horaria para "
                            + fila.getHoraInicio() + " - " + fila.getHoraFin() + ")");
                    continue;
                }

                // 4. EXTRAER VALORES
                var clase = claseOpt.get();
                var franja = franjaOpt.get();

                if (fila.getDiaSemana() == null) {
                    resultados.add("Fila " + filaExcel + ": ERROR (El día de la semana es obligatorio)");
                    continue;
                }
                Short dia = fila.getDiaSemana().shortValue();

                // 5. VALIDAR DUPLICADOS
                boolean existeHorario = repository
                        .existsByAssignedClassId_IdClassAndPeriodId_PeriodIdAndDayAndTimeSlotId_TimeSlotId(
                                clase.getIdClass(),
                                clase.getPeriodId().getPeriodId(),
                                dia,
                                franja.getTimeSlotId());

                if (existeHorario) {
                    resultados.add("Fila " + filaExcel + ": ADVERTENCIA (El horario ya existe, no se duplicó)");
                    continue;
                }

                // 6. GUARDAR NUEVO HORARIO
                ClassSchedule nuevoHorario = new ClassSchedule();
                nuevoHorario.setAssignedClassId(clase);
                nuevoHorario.setPeriodId(clase.getPeriodId());
                nuevoHorario.setTimeSlotId(franja);
                nuevoHorario.setDay(dia);
                nuevoHorario.setActive(true);

                repository.save(nuevoHorario);
                resultados.add("Fila " + filaExcel + ": OK");

            } catch (Exception e) {
                // Atrapamos el error e imprimimos la traza en consola para que sepas
                // exactamente qué falló
                e.printStackTrace();
                resultados.add("Fila " + filaExcel + ": ERROR INTERNO (" + e.getMessage() + ")");
            }
        }
        return resultados;
    }

    @Override
    public List<ClassScheduleDetailDTO> findByUserId(Integer userId) {
        return repository.findByAssignedClassId_TeacherId_UserId_UserId(userId)
                .stream()
                .map(this::toDetailDTO)
                .collect(Collectors.toList());
    }

    private ClassScheduleDetailDTO toDetailDTO(ClassSchedule entity) {
        ClassScheduleDetailDTO dto = new ClassScheduleDetailDTO();
        dto.setIdClassSchedule(entity.getIdClassSchedule());
        dto.setDay(entity.getDay());
        dto.setActive(entity.getActive());

        // Franja horaria
        if (entity.getTimeSlotId() != null) {
            dto.setTimeSlotId(entity.getTimeSlotId().getTimeSlotId());
            dto.setStartTime(entity.getTimeSlotId().getStartTime());
            dto.setEndTime(entity.getTimeSlotId().getEndTime());
        }

        // Clase asignada
        if (entity.getAssignedClassId() != null) {
            var clase = entity.getAssignedClassId();
            dto.setClassId(clase.getIdClass());

            // Docente
            if (clase.getTeacherId() != null) {
                dto.setTeachingId(clase.getTeacherId().getTeachingId());
                if (clase.getTeacherId().getUserId() != null) {
                    dto.setTeacherFirstName(clase.getTeacherId().getUserId().getFirstName());
                    dto.setTeacherLastName(clase.getTeacherId().getUserId().getLastName());
                    dto.setTeacherIdentification(clase.getTeacherId().getUserId().getIdentification());
                }
            }

            // Asignatura
            if (clase.getSubjectId() != null) {
                dto.setSubjectId(clase.getSubjectId().getIdSubject());
                dto.setSubjectName(clase.getSubjectId().getSubject());
                dto.setSemester(clase.getSubjectId().getSemester());
            }

            // Paralelo
            if (clase.getParallelId() != null) {
                dto.setParallelId(clase.getParallelId().getParallelId());
                dto.setSection(clase.getParallelId().getSection());
            }
        }

        // Periodo
        if (entity.getPeriodId() != null) {
            dto.setPeriodId(entity.getPeriodId().getPeriodId());
            dto.setPeriod(entity.getPeriodId().getPeriod());
            dto.setPeriodStartDate(entity.getPeriodId().getStartDate());
            dto.setPeriodEndDate(entity.getPeriodId().getEndDate());
        }

        return dto;
    }
}

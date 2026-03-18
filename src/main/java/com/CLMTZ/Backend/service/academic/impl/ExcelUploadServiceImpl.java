package com.CLMTZ.Backend.service.academic.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.CLMTZ.Backend.dto.academic.CareerLoadDTO;
import com.CLMTZ.Backend.dto.academic.ClassScheduleLoadDTO;
import com.CLMTZ.Backend.dto.academic.EnrollmentDetailLoadDTO;
import com.CLMTZ.Backend.dto.academic.StudentLoadDTO;
import com.CLMTZ.Backend.dto.academic.SubjectLoadDTO;
import com.CLMTZ.Backend.dto.academic.TeachingDTO;
import com.CLMTZ.Backend.service.academic.ICareerService;
import com.CLMTZ.Backend.service.academic.IClassScheduleService;
import com.CLMTZ.Backend.service.academic.ICoordinationService;
import com.CLMTZ.Backend.service.academic.IEnrollmentDetailService;
import com.CLMTZ.Backend.service.academic.IExcelUploadService;
import com.CLMTZ.Backend.service.academic.ISubjectService;
import com.CLMTZ.Backend.util.ExcelHelper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExcelUploadServiceImpl implements IExcelUploadService {

    private static final Logger log = LoggerFactory.getLogger(ExcelUploadServiceImpl.class);

    private final ICoordinationService coordinationService;
    private final ICareerService careerService;
    private final ISubjectService subjectService;
    private final IEnrollmentDetailService enrollmentDetailService;
    private final IClassScheduleService classScheduleService;

    // =====================================================================
    // CARGA DE ESTUDIANTES
    // =====================================================================

    @Override
    public List<String> uploadStudents(MultipartFile file, String carreraTexto, String modalidadTexto) {
        ExcelHelper.validateExcelFormat(file);

        try {
            String nombreOriginal = file.getOriginalFilename();
            String extension = StringUtils.getFilenameExtension(nombreOriginal);
            List<StudentLoadDTO> allStudents = null;

            // 1. Decidimos qué librería usar según la extensión
            if (extension != null && extension.equalsIgnoreCase("xls")) {
                allStudents = ExcelHelper.readStudentsWithEasyExcel(file.getInputStream(), carreraTexto,
                        modalidadTexto);

            } else if (extension != null && extension.equalsIgnoreCase("xlsx")) {
                allStudents = ExcelHelper.readStudentsWithApachePoi(file.getInputStream(), carreraTexto,
                        modalidadTexto);

            } else {
                throw new IllegalArgumentException("La extensión " + extension + " no es válida.");
            }

            allStudents = deduplicateStudents(allStudents);
            log.info("[UPLOAD-STUDENTS] Procesando {} registros", allStudents.size());

            return coordinationService.uploadStudents(allStudents);

        } catch (Exception e) {
            log.error("[UPLOAD-STUDENTS] Error procesando archivo: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo procesar el archivo: "
                    + file.getOriginalFilename() + ". Error: " + e.getMessage(), e);
        }
    }

    // =====================================================================
    // CARGA DE DOCENTES
    // =====================================================================

    @Override
public List<String> uploadTeachers(MultipartFile file) {
    ExcelHelper.validateExcelFormat(file);
    try {
        String nombreOriginal = file.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(nombreOriginal);
        List<TeachingDTO> allTeachers = null;
        
        if (extension != null && extension.equalsIgnoreCase("xls")) {
            allTeachers = ExcelHelper.readTeachersWithEasyExcel(file.getInputStream());
            
        } else if (extension != null && extension.equalsIgnoreCase("xlsx")) {
            allTeachers = ExcelHelper.readTeachersWithApachePoi(file.getInputStream());
            
        } else {
            throw new IllegalArgumentException("La extensión " + extension + " no es válida.");
        }
        
        // 2. Quitamos duplicados y subimos a la base
        allTeachers = deduplicateTeachers(allTeachers);
        log.info("[UPLOAD-TEACHERS] Procesando {} docentes", allTeachers.size());
        
        return coordinationService.uploadTeachers(allTeachers);
        
    } catch (Exception e) {
        log.error("[UPLOAD-TEACHERS] Error procesando archivo: {}", e.getMessage(), e);
        throw new RuntimeException("Error al procesar docentes: " + e.getMessage(), e);
    }
}

    // =====================================================================
    // CARGA DE CARRERAS
    // =====================================================================

    @Override
    public List<String> uploadCareers(MultipartFile file) {
        ExcelHelper.validateExcelFormat(file);

        try {
            List<CareerLoadDTO> careerDTOs = ExcelHelper.excelToCareers(file.getInputStream());
            return careerService.uploadCareers(careerDTOs);
        } catch (Exception e) {
            log.error("[UPLOAD-CAREERS] Error procesando archivo: {}", e.getMessage(), e);
            throw new RuntimeException("Error interno al procesar el archivo: " + e.getMessage(), e);
        }
    }

    // =====================================================================
    // CARGA DE ASIGNATURAS
    // =====================================================================

    @Override
    public List<String> uploadSubjects(MultipartFile file) {
        ExcelHelper.validateExcelFormat(file);

        try {
            List<SubjectLoadDTO> subjectDTOs = ExcelHelper.excelToSubjects(file.getInputStream());
            return subjectService.uploadSubjects(subjectDTOs);
        } catch (Exception e) {
            log.error("[UPLOAD-SUBJECTS] Error procesando archivo: {}", e.getMessage(), e);
            throw new RuntimeException("Error interno al procesar el archivo de asignaturas: " + e.getMessage(), e);
        }
    }

    // =====================================================================
    // CARGA DE MATRÍCULAS
    // =====================================================================

    @Override
    public List<String> uploadRegistrations(MultipartFile file) {
        ExcelHelper.validateExcelFormat(file);
        try {
            String nombreOriginal = file.getOriginalFilename();
            String extension = StringUtils.getFilenameExtension(nombreOriginal);
            List<EnrollmentDetailLoadDTO> registrationDTOs = null;
            if(extension != null && extension.equalsIgnoreCase("xls")){
                registrationDTOs = ExcelHelper.readWithEasyExcel(file.getInputStream());
            }
            else if (extension != null && extension.equalsIgnoreCase("xlsx")){
                registrationDTOs = ExcelHelper.readWithApachePoi(file.getInputStream());
            }
            
            return enrollmentDetailService.uploadEnrollmentDetails(registrationDTOs);
        } catch (Exception e) {
            log.error("[UPLOAD-REGISTRATIONS] Error procesando archivo: {}", e.getMessage(), e);
            throw new RuntimeException("Error al procesar el archivo de matrículas: " + e.getMessage(), e);
        }
    }

    // =====================================================================
    // CARGA DE HORARIOS
    // =====================================================================

    @Override
    public List<String> uploadClassSchedules(MultipartFile file) {
        ExcelHelper.validateExcelFormat(file);

        try {
            List<ClassScheduleLoadDTO> scheduleDTOs = ExcelHelper.excelToClassSchedules(file.getInputStream());
            return classScheduleService.uploadClassSchedules(scheduleDTOs);
        } catch (Exception e) {
            log.error("[UPLOAD-CLASS-SCHEDULES] Error procesando archivo: {}", e.getMessage(), e);
            throw new RuntimeException("Error al procesar el archivo de horarios: " + e.getMessage(), e);
        }
    }

    // =====================================================================
    // MÉTODOS DE DEDUPLICACIÓN
    // =====================================================================

    private List<StudentLoadDTO> deduplicateStudents(List<StudentLoadDTO> batch) {
        LinkedHashMap<String, StudentLoadDTO> deduped = new LinkedHashMap<>();

        for (StudentLoadDTO student : batch) {
            String key = student.getIdentificacion().toUpperCase().trim();
            deduped.putIfAbsent(key, student);
        }

        List<StudentLoadDTO> result = new ArrayList<>(deduped.values());
        int duplicados = batch.size() - result.size();
        if (duplicados > 0) {
            log.info("[DEDUP-STUDENTS] Se removieron {} estudiantes duplicados en el lote.", duplicados);
        }
        return result;
    }

    private List<TeachingDTO> deduplicateTeachers(List<TeachingDTO> batch) {
        LinkedHashMap<String, TeachingDTO> deduped = new LinkedHashMap<>();

        for (TeachingDTO teacher : batch) {
            String key = (teacher.getNombres() + "|" + teacher.getApellidos() + "|" +
                    teacher.getAsignaturaTexto() + "|" + teacher.getParaleloTexto()).toUpperCase().trim();
            deduped.putIfAbsent(key, teacher);
        }

        List<TeachingDTO> result = new ArrayList<>(deduped.values());
        int duplicados = batch.size() - result.size();
        if (duplicados > 0) {
            log.info("[DEDUP-TEACHERS] Se removieron {} docentes duplicados en el lote.", duplicados);
        }
        return result;
    }
}

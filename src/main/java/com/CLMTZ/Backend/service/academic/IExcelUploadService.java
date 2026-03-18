package com.CLMTZ.Backend.service.academic;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public interface IExcelUploadService {

    List<String> uploadStudents(MultipartFile file, String carreraTexto, String modalidadTexto);

    List<String> uploadTeachers(MultipartFile file);

    List<String> uploadCareers(MultipartFile file);

    List<String> uploadSubjects(MultipartFile file);

    List<String> uploadRegistrations(MultipartFile file);

    List<String> uploadClassSchedules(MultipartFile file);
}

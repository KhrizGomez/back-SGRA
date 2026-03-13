package com.CLMTZ.Backend.dto.reinforcement.student;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentRequestResourcesDTO {
    private Integer requestId;
    private List<String> studentFiles = new ArrayList<>();
    private List<String> teacherResources = new ArrayList<>();
    private String virtualLink;
}


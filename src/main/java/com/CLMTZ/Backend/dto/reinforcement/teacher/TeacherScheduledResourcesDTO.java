package com.CLMTZ.Backend.dto.reinforcement.teacher;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeacherScheduledResourcesDTO {
    private Integer scheduledId;
    private List<String> resources;
}


package com.CLMTZ.Backend.dto.reinforcement.student;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentHistoryRequestsPageDTO {
    private List<StudentHistoryRequestItemDTO> items;
    private Long totalCount;
    private Integer page;
    private Integer size;
}
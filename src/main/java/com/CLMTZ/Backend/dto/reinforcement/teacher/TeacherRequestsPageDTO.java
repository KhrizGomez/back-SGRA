package com.CLMTZ.Backend.dto.reinforcement.teacher;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeacherRequestsPageDTO {
    private List<TeacherRequestItemDTO> items;
    private long totalCount;
    private int page;
    private int size;
    private int totalPages;

    public TeacherRequestsPageDTO(List<TeacherRequestItemDTO> items, long totalCount, int page, int size) {
        this.items = items;
        this.totalCount = totalCount;
        this.page = page;
        this.size = size;
        this.totalPages = size > 0 ? (int) Math.ceil((double) totalCount / size) : 0;
    }
}

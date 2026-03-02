package com.CLMTZ.Backend.service.reinforcement.student;

import com.CLMTZ.Backend.dto.reinforcement.student.StudentMyRequestsChipsDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentMyRequestsPageDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentMyRequestsStatusSummaryDTO;

import java.util.List;

public interface StudentMyRequestsService {
    StudentMyRequestsPageDTO getMyRequests(Integer userId, Integer periodId, Integer statusId, Integer sessionTypeId,
                                           Integer subjectId, String search, Integer page, Integer size);
    StudentMyRequestsChipsDTO getMyRequestsChips(Integer userId, Integer periodId);
    List<StudentMyRequestsStatusSummaryDTO> getMyRequestsSummary(Integer userId, Integer periodId);
}
package com.CLMTZ.Backend.service.reinforcement.student;

import com.CLMTZ.Backend.dto.reinforcement.student.StudentHistoryRequestsPageDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentHistorySessionsPageDTO;

public interface StudentHistoryService {
    StudentHistoryRequestsPageDTO getRequestHistory(Integer userId, Integer periodId, Integer page, Integer size, Integer statusId);
    StudentHistorySessionsPageDTO getPreviousSessions(Integer userId, Integer page, Integer size, Boolean onlyAttended);
}
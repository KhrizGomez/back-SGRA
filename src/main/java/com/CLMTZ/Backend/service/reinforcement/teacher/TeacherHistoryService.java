package com.CLMTZ.Backend.service.reinforcement.teacher;

import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherSessionHistoryDetailDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherSessionHistoryPageDTO;

public interface TeacherHistoryService {
    TeacherSessionHistoryPageDTO getSessionHistory(Integer userId, Integer page, Integer size);
    TeacherSessionHistoryDetailDTO getSessionHistoryDetail(Integer userId, Integer scheduledId);
}

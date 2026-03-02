package com.CLMTZ.Backend.service.reinforcement.teacher;

import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherActionResponseDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherRequestsPageDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherScheduleRequestDTO;

public interface TeacherRequestService {
    TeacherRequestsPageDTO getIncomingRequests(Integer userId, Integer statusId, Integer page, Integer size);
    TeacherActionResponseDTO acceptRequest(Integer userId, Integer requestId, TeacherScheduleRequestDTO dto);
    TeacherActionResponseDTO rejectRequest(Integer userId, Integer requestId, String reason);
    TeacherActionResponseDTO rescheduleRequest(Integer userId, Integer requestId, TeacherScheduleRequestDTO dto);
    TeacherActionResponseDTO cancelSession(Integer userId, Integer scheduledId, String reason);
}

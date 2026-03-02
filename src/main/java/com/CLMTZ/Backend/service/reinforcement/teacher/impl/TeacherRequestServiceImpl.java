package com.CLMTZ.Backend.service.reinforcement.teacher.impl;

import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherActionResponseDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherRequestsPageDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherScheduleRequestDTO;
import com.CLMTZ.Backend.repository.reinforcement.teacher.TeacherRequestRepository;
import com.CLMTZ.Backend.service.reinforcement.teacher.TeacherRequestService;
import org.springframework.stereotype.Service;

@Service
public class TeacherRequestServiceImpl implements TeacherRequestService {

    private final TeacherRequestRepository teacherRequestRepository;

    public TeacherRequestServiceImpl(TeacherRequestRepository teacherRequestRepository) {
        this.teacherRequestRepository = teacherRequestRepository;
    }

    @Override
    public TeacherRequestsPageDTO getIncomingRequests(Integer userId, Integer statusId, Integer page, Integer size) {
        return teacherRequestRepository.getIncomingRequests(userId, statusId, page, size);
    }

    @Override
    public TeacherActionResponseDTO acceptRequest(Integer userId, Integer requestId, TeacherScheduleRequestDTO dto) {
        return teacherRequestRepository.acceptRequest(userId, requestId,
                dto.getScheduledDate(), dto.getTimeSlotId(), dto.getModalityId(),
                dto.getEstimatedDuration(), dto.getReason(), dto.getWorkAreaId());
    }

    @Override
    public TeacherActionResponseDTO rejectRequest(Integer userId, Integer requestId, String reason) {
        return teacherRequestRepository.rejectRequest(userId, requestId, reason);
    }

    @Override
    public TeacherActionResponseDTO rescheduleRequest(Integer userId, Integer requestId, TeacherScheduleRequestDTO dto) {
        return teacherRequestRepository.rescheduleRequest(userId, requestId,
                dto.getScheduledDate(), dto.getTimeSlotId(), dto.getModalityId(),
                dto.getEstimatedDuration(), dto.getReason(), dto.getWorkAreaId());
    }

    @Override
    public TeacherActionResponseDTO cancelSession(Integer userId, Integer scheduledId, String reason) {
        return teacherRequestRepository.cancelSession(userId, scheduledId, reason);
    }
}

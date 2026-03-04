package com.CLMTZ.Backend.service.reinforcement.teacher.impl;

import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherActionResponseDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherRequestsPageDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherScheduleRequestDTO;
import com.CLMTZ.Backend.model.academic.TimeSlot;
import com.CLMTZ.Backend.repository.academic.ITimeSlotRepository;
import com.CLMTZ.Backend.repository.reinforcement.teacher.TeacherRequestRepository;
import com.CLMTZ.Backend.service.reinforcement.teacher.TeacherRequestService;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class TeacherRequestServiceImpl implements TeacherRequestService {

    private final TeacherRequestRepository teacherRequestRepository;
    private final ITimeSlotRepository timeSlotRepository;

    public TeacherRequestServiceImpl(TeacherRequestRepository teacherRequestRepository,
                                     ITimeSlotRepository timeSlotRepository) {
        this.teacherRequestRepository = teacherRequestRepository;
        this.timeSlotRepository = timeSlotRepository;
    }

    // -----------------------------------------------------------------------
    // Helper: find an existing TimeSlot by start+end time or create a new one
    // -----------------------------------------------------------------------
    private Integer resolveTimeSlotId(String startTimeStr, String endTimeStr) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("H:mm");
        LocalTime start = LocalTime.parse(startTimeStr, fmt);
        LocalTime end   = LocalTime.parse(endTimeStr,   fmt);

        Optional<TimeSlot> existing = timeSlotRepository.findByStartTimeAndEndTime(start, end);
        if (existing.isPresent()) {
            return existing.get().getTimeSlotId();
        }

        // No matching slot found – create and persist a new one
        TimeSlot newSlot = new TimeSlot();
        newSlot.setStartTime(start);
        newSlot.setEndTime(end);
        newSlot.setState(true);
        return timeSlotRepository.save(newSlot).getTimeSlotId();
    }

    @Override
    public TeacherRequestsPageDTO getIncomingRequests(Integer userId, Integer statusId, Integer page, Integer size) {
        return teacherRequestRepository.getIncomingRequests(userId, statusId, page, size);
    }

    @Override
    public TeacherActionResponseDTO acceptRequest(Integer userId, Integer requestId, TeacherScheduleRequestDTO dto) {
        Integer timeSlotId = resolveTimeSlotId(dto.getStartTime(), dto.getEndTime());
        return teacherRequestRepository.acceptRequest(userId, requestId,
                dto.getScheduledDate(), timeSlotId, dto.getModalityId(),
                dto.getEstimatedDuration(), dto.getReason(), dto.getWorkAreaTypeId());
    }

    @Override
    public TeacherActionResponseDTO rejectRequest(Integer userId, Integer requestId, String reason) {
        return teacherRequestRepository.rejectRequest(userId, requestId, reason);
    }

    @Override
    public TeacherActionResponseDTO rescheduleRequest(Integer userId, Integer requestId, TeacherScheduleRequestDTO dto) {
        Integer timeSlotId = resolveTimeSlotId(dto.getStartTime(), dto.getEndTime());
        return teacherRequestRepository.rescheduleRequest(userId, requestId,
                dto.getScheduledDate(), timeSlotId, dto.getModalityId(),
                dto.getEstimatedDuration(), dto.getReason(), dto.getWorkAreaTypeId());
    }

    @Override
    public TeacherActionResponseDTO cancelSession(Integer userId, Integer scheduledId, String reason) {
        return teacherRequestRepository.cancelSession(userId, scheduledId, reason);
    }
}

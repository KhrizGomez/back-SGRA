package com.CLMTZ.Backend.service.reinforcement.student.impl;

import com.CLMTZ.Backend.config.UserContextHolder;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentInvitationItemDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentInvitationResponseDTO;
import com.CLMTZ.Backend.repository.reinforcement.student.StudentInvitationRepository;
import com.CLMTZ.Backend.service.reinforcement.student.StudentInvitationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentInvitationServiceImpl implements StudentInvitationService {

    private final StudentInvitationRepository studentInvitationRepository;

    public StudentInvitationServiceImpl(StudentInvitationRepository studentInvitationRepository) {
        this.studentInvitationRepository = studentInvitationRepository;
    }

    @Override
    public List<StudentInvitationItemDTO> getPendingInvitations() {
        Integer userId = UserContextHolder.getContext().getUserId();
        return studentInvitationRepository.listPendingInvitations(userId);
    }

    @Override
    public StudentInvitationResponseDTO respondInvitation(Integer participantId, Boolean accept) {
        Integer userId = UserContextHolder.getContext().getUserId();
        Boolean success = studentInvitationRepository.respondInvitation(userId, participantId, accept);

        if (success) {
            String message = accept ? "Has aceptado la invitación a la tutoría grupal"
                                    : "Has rechazado la invitación a la tutoría grupal";
            return new StudentInvitationResponseDTO(true, message);
        } else {
            return new StudentInvitationResponseDTO(false,
                    "No se pudo procesar la invitación. Verifica que aún esté pendiente.");
        }
    }
}

package com.CLMTZ.Backend.service.reinforcement.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.CLMTZ.Backend.dto.reinforcement.ReinforcementRequestDTO;
import com.CLMTZ.Backend.model.reinforcement.ReinforcementRequest;
import com.CLMTZ.Backend.repository.reinforcement.IReinforcementRequestRepository;
import com.CLMTZ.Backend.repository.reinforcement.IReinforcementRequestStatusRepository;
import com.CLMTZ.Backend.repository.academic.IStudentsRepository;
import com.CLMTZ.Backend.repository.academic.ITeachingRepository;
import com.CLMTZ.Backend.repository.academic.ITimeSlotRepository;
import com.CLMTZ.Backend.repository.academic.IModalityRepository;
import com.CLMTZ.Backend.repository.academic.IPeriodRepository;
import com.CLMTZ.Backend.repository.reinforcement.ISessionTypesRepository;
import com.CLMTZ.Backend.service.reinforcement.IReinforcementRequestService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReinforcementRequestServiceImpl implements IReinforcementRequestService {

}

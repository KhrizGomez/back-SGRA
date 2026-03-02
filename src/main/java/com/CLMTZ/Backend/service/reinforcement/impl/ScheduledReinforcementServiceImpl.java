package com.CLMTZ.Backend.service.reinforcement.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.CLMTZ.Backend.dto.reinforcement.ScheduledReinforcementDTO;
import com.CLMTZ.Backend.model.reinforcement.ScheduledReinforcement;
import com.CLMTZ.Backend.repository.reinforcement.IScheduledReinforcementRepository;
import com.CLMTZ.Backend.repository.academic.IModalityRepository;
import com.CLMTZ.Backend.repository.academic.ITimeSlotRepository;
import com.CLMTZ.Backend.repository.reinforcement.ISessionTypesRepository;
import com.CLMTZ.Backend.service.reinforcement.IScheduledReinforcementService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScheduledReinforcementServiceImpl implements IScheduledReinforcementService {
    
}

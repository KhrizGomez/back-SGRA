package com.CLMTZ.Backend.service.reinforcement.coordination.impl;

import com.CLMTZ.Backend.dto.reinforcement.coordination.CoordinationDashboardDTO;
import com.CLMTZ.Backend.repository.reinforcement.coordination.CoordinationDashboardRepository;
import com.CLMTZ.Backend.service.reinforcement.coordination.CoordinationDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CoordinationDashboardServiceImpl implements CoordinationDashboardService {

    private final CoordinationDashboardRepository repository;

    @Override
    public CoordinationDashboardDTO getDashboard() {
        return repository.getDashboard();
    }
}

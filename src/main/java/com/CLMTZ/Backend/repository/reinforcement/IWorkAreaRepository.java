package com.CLMTZ.Backend.repository.reinforcement;

import org.springframework.data.jpa.repository.JpaRepository;

import com.CLMTZ.Backend.model.reinforcement.WorkArea;
import java.util.List;


public interface IWorkAreaRepository extends JpaRepository<WorkArea, Integer> {
    List<WorkArea> findByWorkAreaTypeId(Integer workAreaTypeId);
}

package com.CLMTZ.Backend.repository.reinforcement.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.CLMTZ.Backend.model.reinforcement.ScheduledReinforcement;

public interface IScheduledReinforcementRepository extends JpaRepository<ScheduledReinforcement, Integer> {

    /**
     * Llama a la función de BD que determina qué sesiones futuras activas
     * corresponden revisar para recordatorios. Retorna solo los IDs.
     */
    @Query(value = "SELECT * FROM reforzamiento.fn_get_ids_sesiones_proximas()", nativeQuery = true)
    List<Integer> findIdsSesionesFuturas();
}

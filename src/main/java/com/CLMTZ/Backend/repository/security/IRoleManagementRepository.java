package com.CLMTZ.Backend.repository.security;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.CLMTZ.Backend.model.security.RoleManagement;
@Repository
public interface IRoleManagementRepository extends JpaRepository<RoleManagement, Integer> {
    List<RoleManagement> findByStateTrue();

    Long countByState(Boolean estado);
}
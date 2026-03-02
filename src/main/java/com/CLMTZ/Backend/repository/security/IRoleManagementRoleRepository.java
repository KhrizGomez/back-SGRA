package com.CLMTZ.Backend.repository.security;

import org.springframework.data.jpa.repository.JpaRepository;
import com.CLMTZ.Backend.model.security.RoleManagementRole;

public interface IRoleManagementRoleRepository extends JpaRepository<RoleManagementRole, Integer> {

}

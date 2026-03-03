package com.CLMTZ.Backend.repository.security.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.CLMTZ.Backend.model.security.UserRoleManagement;

public interface IUserRoleManagementRepository extends JpaRepository<UserRoleManagement, Integer> {

}

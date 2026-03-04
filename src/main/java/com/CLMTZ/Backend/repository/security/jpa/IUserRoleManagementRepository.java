package com.CLMTZ.Backend.repository.security.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.CLMTZ.Backend.model.security.UserRoleManagement;

@Repository
public interface IUserRoleManagementRepository extends JpaRepository<UserRoleManagement, Integer> {

}

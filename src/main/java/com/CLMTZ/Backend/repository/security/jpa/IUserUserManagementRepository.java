package com.CLMTZ.Backend.repository.security.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.CLMTZ.Backend.model.security.UserUserManagement;

public interface IUserUserManagementRepository extends JpaRepository<UserUserManagement, Integer> {

}

package com.CLMTZ.Backend.repository.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.CLMTZ.Backend.model.security.UserManagement;
@Repository
public interface IUserManagementRepository extends JpaRepository<UserManagement, Integer> {
    Long countByState(Boolean state);
}

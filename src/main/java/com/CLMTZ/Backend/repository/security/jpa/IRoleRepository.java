package com.CLMTZ.Backend.repository.security.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.CLMTZ.Backend.model.security.Role;
import java.util.List;


public interface IRoleRepository extends JpaRepository<Role, Integer> {
    List<Role> findByState(Boolean state);
}

package com.CLMTZ.Backend.repository.security.jpa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.CLMTZ.Backend.model.security.Access;

public interface IAccessRepository extends JpaRepository<Access, Integer> {
    Optional<Access> findByUsername(String username);
}

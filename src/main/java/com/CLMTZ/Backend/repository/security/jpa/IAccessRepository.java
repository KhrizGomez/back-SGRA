package com.CLMTZ.Backend.repository.security.jpa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.CLMTZ.Backend.model.security.Access;

@Repository
public interface IAccessRepository extends JpaRepository<Access, Integer> {
    Optional<Access> findByUsername(String username);
    boolean existsByUser_UserId(Integer userId);
}

package com.CLMTZ.Backend.repository.security.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.CLMTZ.Backend.model.security.AccessAudit;


@Repository
public interface IAccessAuditRepository extends JpaRepository<AccessAudit, Integer>{

    AccessAudit findBySession(String session);

}
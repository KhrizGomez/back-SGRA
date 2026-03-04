package com.CLMTZ.Backend.repository.security.jpa;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.CLMTZ.Backend.model.security.UsersRoles;

@Repository
public interface IUsersRolesRepository extends JpaRepository<UsersRoles, Integer> {

    @Query("SELECT ur FROM UsersRoles ur JOIN FETCH ur.roleId WHERE ur.userId.userId = :userId AND ur.state = true")
    List<UsersRoles> findActiveRolesByUserId(@Param("userId") Integer userId);
}

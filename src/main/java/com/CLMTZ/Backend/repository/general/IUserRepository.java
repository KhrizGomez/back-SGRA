package com.CLMTZ.Backend.repository.general;

import org.springframework.data.jpa.repository.JpaRepository;

import com.CLMTZ.Backend.model.general.User;

public interface IUserRepository extends JpaRepository<User, Integer> {

}

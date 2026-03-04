package com.CLMTZ.Backend.repository.general;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.CLMTZ.Backend.model.general.User;

public interface IUserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByIdentification(String identification);

    Optional<User> findByFirstNameAndLastName(String firstName, String lastName);

    Optional<User> findByEmail(String email);
}

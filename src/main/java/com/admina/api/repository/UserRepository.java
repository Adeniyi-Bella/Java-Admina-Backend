package com.admina.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.admina.api.model.user.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
}

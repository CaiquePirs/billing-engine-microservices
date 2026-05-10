package com.authentication.repository;

import com.authentication.model.Authentication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthenticationRepository extends JpaRepository<Authentication, UUID> {

    Optional<Authentication> findByEmail(String email);
}

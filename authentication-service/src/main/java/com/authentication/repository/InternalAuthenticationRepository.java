package com.authentication.repository;

import com.authentication.model.InternalAuthentication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InternalAuthenticationRepository extends JpaRepository<InternalAuthentication, UUID> {
    Optional<InternalAuthentication> findByClientId(String clientId);
    Boolean existsByClientId(String clientId);
}

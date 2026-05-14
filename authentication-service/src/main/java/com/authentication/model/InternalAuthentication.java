package com.authentication.model;

import com.authentication.model.enums.AuthScope;
import com.authentication.model.enums.AuthStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InternalAuthentication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "client_id", nullable = false, unique = true)
    private String clientId;

    @Column(name = "client_secret_hash", nullable = false)
    private String clientSecretHash;

    @Column(name = "scope", nullable = false)
    @Enumerated(EnumType.STRING)
    private AuthScope scope;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private AuthStatus status;

    @Embedded
    private AuditEntity auditEntity;
}

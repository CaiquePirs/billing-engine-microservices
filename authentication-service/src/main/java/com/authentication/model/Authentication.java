package com.authentication.model;

import com.authentication.model.enums.AuthStatus;
import com.authentication.model.enums.Role;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "authentication")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Authentication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private AuthStatus status;

    @Column(name = "auth_role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Embedded
    private AuditEntity auditEntity;
}

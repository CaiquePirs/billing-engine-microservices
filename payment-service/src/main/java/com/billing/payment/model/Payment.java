package com.billing.payment.model;

import com.billing.payment.model.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "stripe_payment_intent_id", nullable = false)
    private String stripePaymentIntentId;

    @Column(name = "stripe_event_id", nullable = false, unique = true)
    private String stripeEventId; // idempotência

    @Column(name = "amount", nullable = false)
    private Long amount; // em centavos

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", nullable = false, columnDefinition = "jsonb")
    private String rawPayload;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Embedded
    private AuditLog auditLog;
}
package com.billing.invoice.service;

import com.billing.invoice.events.data.CustomerAddressResponse;
import com.billing.invoice.events.data.CustomerClientResponse;
import com.billing.invoice.events.data.PlanResponseDTO;
import com.billing.invoice.events.data.SubscriptionPaymentEvent;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class InvoicePdfGeneratorTest {

    private final InvoicePdfGenerator generator = new InvoicePdfGenerator();

    @Test
    void shouldGenerateInvoicePdfSuccessfully() {
        // Arrange
        UUID invoiceId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        
        CustomerAddressResponse address = new CustomerAddressResponse(
                UUID.randomUUID(),
                "Main Street",
                "123",
                "Dublin",
                "Co. Dublin",
                "Ireland",
                "D01 1AA"
        );

        CustomerClientResponse customer = new CustomerClientResponse(
                UUID.randomUUID(),
                "John",
                "Doe",
                "john.doe@example.com",
                "+35312345678",
                address,
                "cus_123456"
        );

        PlanResponseDTO plan = new PlanResponseDTO(
                UUID.randomUUID(),
                "Premium Plan",
                "Description",
                new BigDecimal("49.90"),
                "EUR",
                "month",
                "price_123",
                true
        );

        SubscriptionPaymentEvent event = new SubscriptionPaymentEvent(
                paymentId,
                subscriptionId,
                "APPROVED",
                "ACTIVE",
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                plan,
                customer
        );

        // Act
        byte[] pdfBytes = generator.generate(invoiceId, event);

        // Assert
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }
}

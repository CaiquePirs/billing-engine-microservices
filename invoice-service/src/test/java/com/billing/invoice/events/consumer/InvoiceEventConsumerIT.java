package com.billing.invoice.events.consumer;

import com.billing.invoice.advice.exceptions.InternalErrorException;
import com.billing.invoice.events.data.CustomerAddressResponse;
import com.billing.invoice.events.data.CustomerClientResponse;
import com.billing.invoice.events.data.PlanResponseDTO;
import com.billing.invoice.events.data.SnsMessage;
import com.billing.invoice.events.data.SubscriptionPaymentEvent;
import com.billing.invoice.service.InvoiceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "eureka.client.enabled=false",
        "spring.cloud.aws.sqs.enabled=false",
        "aws.sqs.queue.generate-invoice=test-queue",
        "aws.sqs.queue.invoice-created=test-invoice-created-queue",
        "aws.localstack.access-key=test",
        "aws.localstack.secret-key=test",
        "aws.localstack.region=us-east-1",
        "aws.localstack.endpoint=http://localhost:4566",
        "aws.s3.access-key=test",
        "aws.s3.secret-key=test",
        "aws.s3.region=us-east-1",
        "aws.s3.endpoint-override=http://localhost:4566",
        "aws.s3.bucket=test-bucket",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class InvoiceEventConsumerIT {

    @Autowired private InvoiceEventConsumer invoiceEventConsumer;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private InvoiceService invoiceService;

    private SubscriptionPaymentEvent buildEvent() {
        CustomerAddressResponse address = new CustomerAddressResponse(
                UUID.randomUUID(), "Main St", "10", "Dublin", "Leinster", "Co. Dublin", "D01AB12");
        CustomerClientResponse customer = new CustomerClientResponse(
                UUID.randomUUID(), "John", "Doe", "john@example.com", "+353123456789", address, "cus_test");
        PlanResponseDTO plan = new PlanResponseDTO(
                UUID.randomUUID(), "Premium", "Premium plan", new BigDecimal("9900"), "EUR", "MONTHLY", null, true);
        return SubscriptionPaymentEvent.builder()
                .paymentId(UUID.randomUUID()).subscriptionId(UUID.randomUUID())
                .paymentStatus("APPROVED").subscriptionStatus("ACTIVE")
                .currentPeriodStart(LocalDate.now()).currentPeriodEnd(LocalDate.now().plusMonths(1))
                .customer(customer).plan(plan).build();
    }

    @Test
    void generateNewInvoiceQueue_shouldCallInvoiceService_whenEventIsValid() throws Exception {
        SubscriptionPaymentEvent event = buildEvent();
        String messageBody = objectMapper.writeValueAsString(event);
        SnsMessage snsMessage = new SnsMessage("Notification", messageBody);

        doNothing().when(invoiceService).generateInvoice(any());

        invoiceEventConsumer.generateNewInvoiceQueue(snsMessage);

        verify(invoiceService).generateInvoice(any(SubscriptionPaymentEvent.class));
    }

    @Test
    void generateNewInvoiceQueue_shouldThrowInternalErrorException_whenDeserializationFails() {
        SnsMessage snsMessage = new SnsMessage("Notification", "not-valid-json");

        assertThatThrownBy(() -> invoiceEventConsumer.generateNewInvoiceQueue(snsMessage))
                .isInstanceOf(InternalErrorException.class)
                .hasMessageContaining("Failed to generate invoice");

        verify(invoiceService, never()).generateInvoice(any());
    }

    @Test
    void generateNewInvoiceQueue_shouldThrowInternalErrorException_whenInvoiceServiceFails() throws Exception {
        SubscriptionPaymentEvent event = buildEvent();
        String messageBody = objectMapper.writeValueAsString(event);
        SnsMessage snsMessage = new SnsMessage("Notification", messageBody);

        doThrow(new RuntimeException("PDF generation failed")).when(invoiceService).generateInvoice(any());

        assertThatThrownBy(() -> invoiceEventConsumer.generateNewInvoiceQueue(snsMessage))
                .isInstanceOf(InternalErrorException.class)
                .hasMessageContaining("Failed to generate invoice");
    }
}

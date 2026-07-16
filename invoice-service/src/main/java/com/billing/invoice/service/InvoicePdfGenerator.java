package com.billing.invoice.service;

import com.billing.invoice.advice.exceptions.InternalErrorException;
import com.billing.invoice.events.data.CustomerAddressResponse;
import com.billing.invoice.events.data.CustomerClientResponse;
import com.billing.invoice.events.data.PlanResponseDTO;
import com.billing.invoice.events.data.SubscriptionPaymentEvent;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Component
public class InvoicePdfGenerator {

    private static final String TEMPLATE_PATH = "templates/invoice.html";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generate(UUID invoiceId, SubscriptionPaymentEvent event) {
        try {
            String template = loadTemplate();
            String html = populateTemplate(template, invoiceId, event);

            return renderToPdf(html);
        } catch (Exception e) {
            throw new InternalErrorException("Failed to generate invoice PDF for invoiceId: " + invoiceId, e);
        }
    }

    private String loadTemplate() throws IOException {
        ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String populateTemplate(String template, UUID invoiceId, SubscriptionPaymentEvent event) {
        CustomerClientResponse customer = event.customer();
        CustomerAddressResponse address = customer.address();
        PlanResponseDTO plan = event.plan();

        String customerName = customer.name();
        String formattedPrice = String.format("%.2f", plan.price().divide(new java.math.BigDecimal(100)));
        String issueDate = LocalDate.now().format(DATE_FORMATTER);

        return template
                .replace("{{invoiceId}}", invoiceId.toString().toUpperCase())
                .replace("{{issueDate}}", issueDate)
                .replace("{{dueDate}}", format(event.currentPeriodEnd()))
                .replace("{{paymentStatus}}", event.paymentStatus())
                // Customer
                .replace("{{customerName}}", customerName)
                .replace("{{customerEmail}}", customer.email())
                .replace("{{customerStreet}}", address != null ? orEmpty(address.street()) : "")
                .replace("{{customerNumber}}", address != null ? orEmpty(address.number()) : "")
                .replace("{{customerCity}}", address != null ? orEmpty(address.city()) : "")
                .replace("{{customerState}}", address != null ? orEmpty(address.state()) : "")
                .replace("{{customerCountry}}", address != null ? orEmpty(address.county()) : "")
                .replace("{{customerEircode}}", address != null ? orEmpty(address.eircode()) : "")
                // Subscription
                .replace("{{subscriptionId}}", event.subscriptionId().toString())
                .replace("{{planName}}", plan.name())
                .replace("{{planDescription}}", orEmpty(plan.description()))
                .replace("{{billingInterval}}", plan.interval())
                .replace("{{billingPeriodStart}}", format(event.currentPeriodStart()))
                .replace("{{billingPeriodEnd}}", format(event.currentPeriodEnd()))
                // Pricing
                .replace("{{currency}}", plan.currency().toUpperCase())
                .replace("{{unitPrice}}", formattedPrice)
                .replace("{{amount}}", formattedPrice)
                // Payment
                .replace("{{paymentId}}", event.paymentId().toString());
    }

    private byte[] renderToPdf(String html) throws Exception {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        }
    }

    private String format(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "";
    }

    private String orEmpty(String value) {
        return value != null ? value : "";
    }
}

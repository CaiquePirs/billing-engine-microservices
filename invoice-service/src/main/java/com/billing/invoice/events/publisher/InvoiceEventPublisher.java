package com.billing.invoice.events.publisher;

import com.billing.invoice.repository.InvoiceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceEventPublisher {

    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;
    private final InvoiceRepository invoiceRepository;




}

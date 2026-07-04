package com.billing.invoice.service;

import com.billing.invoice.advice.exceptions.InternalErrorException;
import com.billing.invoice.metrics.InvoiceMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceStorageServiceTest {

    @Mock private S3Client s3Client;
    @Mock private InvoiceMetrics invoiceMetrics;

    @InjectMocks
    private InvoiceStorageService invoiceStorageService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(invoiceStorageService, "bucket", "test-bucket");
    }

    @Test
    void uploadInvoicePdf_shouldUploadAndReturnS3Key_whenUploadSucceeds() {
        UUID invoiceId = UUID.randomUUID();
        byte[] pdfBytes = "pdf-content".getBytes();

        String s3Key = invoiceStorageService.uploadInvoicePdf(invoiceId, pdfBytes);

        assertThat(s3Key).isEqualTo("invoices/" + invoiceId + ".pdf");
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(invoiceMetrics).recordInvoicePdfUploadedToS3Total();
        verify(invoiceMetrics, never()).recordInvoicePdfUploadToS3FailedTotal();
    }

    @Test
    void uploadInvoicePdf_shouldThrowInternalErrorExceptionAndRecordFailure_whenS3ClientFails() {
        UUID invoiceId = UUID.randomUUID();
        byte[] pdfBytes = "pdf-content".getBytes();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("S3 error"));

        assertThatThrownBy(() -> invoiceStorageService.uploadInvoicePdf(invoiceId, pdfBytes))
                .isInstanceOf(InternalErrorException.class)
                .hasMessageContaining("Failed to upload invoice PDF to S3 for invoiceId: " + invoiceId);

        verify(invoiceMetrics).recordInvoicePdfUploadToS3FailedTotal();
        verify(invoiceMetrics, never()).recordInvoicePdfUploadedToS3Total();
    }
}

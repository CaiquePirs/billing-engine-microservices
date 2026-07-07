package com.billing.invoice.service;

import com.billing.invoice.advice.exceptions.InternalErrorException;
import com.billing.invoice.metrics.InvoiceMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceStorageService {

    private final S3Client s3Client;
    private final InvoiceMetrics invoiceMetrics;

    @Value("${aws.s3.bucket}")
    private String bucket;

    public String uploadInvoicePdf(UUID invoiceId, byte[] pdfBytes) {
        String s3Key = buildS3Key(invoiceId);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .contentType("application/pdf")
                    .contentLength((long) pdfBytes.length)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(pdfBytes));
            invoiceMetrics.recordInvoicePdfUploadedToS3Total();
            log.info("Invoice PDF uploaded to S3 (invoiceId={}, s3Key={})", invoiceId, s3Key);

            return s3Key;

        } catch (Exception e) {
            invoiceMetrics.recordInvoicePdfUploadToS3FailedTotal();
            throw new InternalErrorException("Failed to upload invoice PDF to S3 for invoiceId: " + invoiceId, e);
        }
    }

    private String buildS3Key(UUID invoiceId) {
        return "invoices/" + invoiceId + ".pdf";
    }
}

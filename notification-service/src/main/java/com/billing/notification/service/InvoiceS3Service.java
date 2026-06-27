package com.billing.notification.service;

import com.billing.notification.advice.exceptions.InternalNotificationErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceS3Service {

    private final S3Client s3Client;

    @Value("${AWS_S3_BUCKET}")
    private String bucket;

    public byte[] downloadPdf(String s3Key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build();

            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
            return response.asByteArray();

        } catch (Exception e) {
            log.error("Failed to download invoice PDF from S3. Bucket: {}, Key: {}", bucket, s3Key, e);
            throw new InternalNotificationErrorException("Failed to retrieve invoice PDF from S3 for key: " + s3Key, e);
        }
    }
}

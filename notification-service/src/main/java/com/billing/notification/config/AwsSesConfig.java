package com.billing.notification.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesAsyncClient;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration
public class AwsSesConfig {

    @Value("${aws.production.region}")
    private String region;

    @Value("${aws.production.access-key}")
    private String accessKey;

    @Value("${aws.production.secret-key}")
    private String secretKey;

    @Bean
    public SesClient sesClient() {
        return SesClient.builder()
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        accessKey,
                                        secretKey
                                )
                        )
                )
                .build();
    }

    @Bean
    public SesAsyncClient sesAsyncClient() {
        return SesAsyncClient.builder()
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        accessKey,
                                        secretKey
                                )
                        )
                )
                .build();
    }
}
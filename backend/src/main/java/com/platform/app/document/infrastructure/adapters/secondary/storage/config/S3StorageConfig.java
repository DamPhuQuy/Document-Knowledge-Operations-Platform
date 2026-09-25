package com.platform.app.document.infrastructure.adapters.secondary.storage.config;

import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

@Configuration
@Slf4j
public class S3StorageConfig {

  @Bean
  public S3Client s3Client(S3StorageProperties properties) {
    S3ClientBuilder builder =
        S3Client.builder()
            .region(Region.of(properties.getRegion()))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        properties.getAccessKeyId(), properties.getSecretAccessKey())))
            .serviceConfiguration(
                S3Configuration.builder()
                    .pathStyleAccessEnabled(properties.isPathStyleAccess())
                    .build());

    if (properties.getEndpoint() != null && !properties.getEndpoint().trim().isEmpty()) {
      builder.endpointOverride(URI.create(properties.getEndpoint().trim()));
    }

    return builder.build();
  }

  @Bean
  @ConditionalOnProperty(
      name = "aws.s3.auto-create-bucket",
      havingValue = "true",
      matchIfMissing = true)
  public ApplicationRunner s3BucketInitializer(
      S3Client s3Client, S3StorageProperties properties) {
    return args -> {
      String bucket = properties.getBucketName();
      if (bucket == null || bucket.isBlank()) {
        return;
      }
      try {
        s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        log.info("S3 bucket [{}] exists and is accessible", bucket);
      } catch (NoSuchBucketException ex) {
        log.info("S3 bucket [{}] does not exist, creating bucket...", bucket);
        s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        log.info("S3 bucket [{}] created successfully", bucket);
      } catch (Exception ex) {
        log.warn("Could not verify or auto-create S3 bucket [{}]: {}", bucket, ex.getMessage());
      }
    };
  }
}

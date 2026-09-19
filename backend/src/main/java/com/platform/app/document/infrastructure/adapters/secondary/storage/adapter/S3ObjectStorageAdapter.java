package com.platform.app.document.infrastructure.adapters.secondary.storage.adapter;

import com.platform.app.document.application.ports.outbound.ObjectStoragePort;
import com.platform.app.document.domain.exception.StorageException;
import com.platform.app.document.infrastructure.adapters.secondary.storage.config.S3StorageProperties;
import java.io.InputStream;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@RequiredArgsConstructor
@Slf4j
public class S3ObjectStorageAdapter implements ObjectStoragePort {

    private final S3Client s3Client;
    private final S3StorageProperties properties;

    @Override
    public void upload(
        String storageKey,
        InputStream inputStream,
        long contentLength,
        String contentType
    ) {
        Objects.requireNonNull(storageKey, "Storage key must not be null");
        Objects.requireNonNull(inputStream, "InputStream must not be null");

        String bucket = properties.getBucketName();
        log.info(
            "Uploading object to S3: bucket={}, key={}, size={}, contentType={}",
            bucket,
            storageKey,
            contentLength,
            contentType
        );

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .contentType(
                    contentType != null
                        ? contentType
                        : "application/octet-stream"
                )
                .contentLength(contentLength)
                .build();

            s3Client.putObject(
                putRequest,
                RequestBody.fromInputStream(inputStream, contentLength)
            );
            log.info(
                "Successfully uploaded object to S3: bucket={}, key={}",
                bucket,
                storageKey
            );
        } catch (SdkException e) {
            log.error(
                "Error uploading object to S3: bucket={}, key={}, error={}",
                bucket,
                storageKey,
                e.getMessage(),
                e
            );
            throw new StorageException(
                "Failed to upload object to S3: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public void delete(String storageKey) {
        if (storageKey == null || storageKey.trim().isEmpty()) {
            log.warn("Attempted to delete S3 object with blank storage key");
            return;
        }

        String bucket = properties.getBucketName();
        log.info(
            "Deleting object from S3: bucket={}, key={}",
            bucket,
            storageKey
        );

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .build();

            s3Client.deleteObject(deleteRequest);
            log.info(
                "Successfully deleted object from S3: bucket={}, key={}",
                bucket,
                storageKey
            );
        } catch (SdkException e) {
            log.error(
                "Error deleting object from S3: bucket={}, key={}, error={}",
                bucket,
                storageKey,
                e.getMessage(),
                e
            );
            throw new StorageException(
                "Failed to delete object from S3: " + e.getMessage(),
                e
            );
        }
    }
}

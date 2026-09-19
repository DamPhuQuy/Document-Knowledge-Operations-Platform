package com.platform.app.document.infrastructure.adapters.secondary.storage.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.platform.app.document.domain.exception.StorageException;
import com.platform.app.document.infrastructure.adapters.secondary.storage.config.S3StorageProperties;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@ExtendWith(MockitoExtension.class)
class S3ObjectStorageAdapterTest {

    @Mock
    private S3Client s3Client;

    private S3StorageProperties properties;
    private S3ObjectStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        properties = new S3StorageProperties();
        properties.setBucketName("test-bucket");
        adapter = new S3ObjectStorageAdapter(s3Client, properties);
    }

    @Test
    @DisplayName(
        "Should upload object to S3 with correct bucket, key and content type"
    )
    void shouldUploadObjectSuccessfully() {
        String key = "documents/123/v1/test.pdf";
        byte[] content = "test file content".getBytes();
        InputStream stream = new ByteArrayInputStream(content);

        when(
            s3Client.putObject(
                any(PutObjectRequest.class),
                any(RequestBody.class)
            )
        ).thenReturn(PutObjectResponse.builder().build());

        adapter.upload(key, stream, content.length, "application/pdf");

        ArgumentCaptor<PutObjectRequest> requestCaptor =
            ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(
            requestCaptor.capture(),
            any(RequestBody.class)
        );

        PutObjectRequest captured = requestCaptor.getValue();
        assertEquals("test-bucket", captured.bucket());
        assertEquals(key, captured.key());
        assertEquals("application/pdf", captured.contentType());
        assertEquals((long) content.length, captured.contentLength());
    }

    @Test
    @DisplayName(
        "Should wrap SdkException into StorageException when upload fails"
    )
    void shouldThrowStorageExceptionWhenUploadFails() {
        String key = "documents/123/v1/test.pdf";
        byte[] content = "test content".getBytes();
        InputStream stream = new ByteArrayInputStream(content);

        when(
            s3Client.putObject(
                any(PutObjectRequest.class),
                any(RequestBody.class)
            )
        ).thenThrow(SdkClientException.create("S3 connection timeout"));

        assertThrows(StorageException.class, () ->
            adapter.upload(key, stream, content.length, "application/pdf")
        );
    }

    @Test
    @DisplayName("Should delete object from S3 successfully")
    void shouldDeleteObjectSuccessfully() {
        String key = "documents/123/v1/test.pdf";
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(
            DeleteObjectResponse.builder().build()
        );

        adapter.delete(key);

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(
            DeleteObjectRequest.class
        );
        verify(s3Client).deleteObject(captor.capture());
        assertEquals("test-bucket", captor.getValue().bucket());
        assertEquals(key, captor.getValue().key());
    }

    @Test
    @DisplayName(
        "Should wrap SdkException into StorageException when delete fails"
    )
    void shouldThrowStorageExceptionWhenDeleteFails() {
        String key = "documents/123/v1/test.pdf";
        doThrow(SdkClientException.create("S3 connection error"))
            .when(s3Client)
            .deleteObject(any(DeleteObjectRequest.class));

        assertThrows(StorageException.class, () -> adapter.delete(key));
    }
}

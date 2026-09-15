package com.platform.app.document.infrastructure.adapters.secondary.storage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "aws.s3")
public class S3StorageProperties {

  private String endpoint;
  private String region = "us-east-1";
  private String bucketName = "doc-knowledge-storage";
  private String accessKeyId = "test";
  private String secretAccessKey = "test";
  private boolean pathStyleAccess = true;
}

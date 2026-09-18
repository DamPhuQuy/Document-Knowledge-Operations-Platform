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
  private String region;
  private String bucketName;
  private String accessKeyId;
  private String secretAccessKey;
  private boolean pathStyleAccess;
}

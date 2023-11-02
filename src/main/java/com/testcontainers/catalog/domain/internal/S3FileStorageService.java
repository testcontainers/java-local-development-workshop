package com.testcontainers.catalog.domain.internal;

import com.testcontainers.catalog.ApplicationProperties;
import com.testcontainers.catalog.domain.FileStorageService;
import io.awspring.cloud.s3.S3Template;
import java.io.InputStream;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class S3FileStorageService implements FileStorageService {
    private static final Logger log = LoggerFactory.getLogger(S3FileStorageService.class);
    private final S3Template s3Template;
    private final ApplicationProperties properties;

    public S3FileStorageService(S3Template s3Template, ApplicationProperties properties) {
        this.s3Template = s3Template;
        this.properties = properties;
    }

    public void createBucket(String bucketName) {
        s3Template.createBucket(bucketName);
    }

    public void upload(String filename, InputStream inputStream) {
        log.debug("Uploading file with name {} to S3", filename);
        try {
            s3Template.upload(properties.productImagesBucketName(), filename, inputStream, null);
            log.debug("Uploaded file with name {} to S3", filename);
        } catch (Exception e) {
            log.error("IException: ", e);
            throw new RuntimeException(e);
        }
    }

    public String getPreSignedURL(String filename) {
        return s3Template
                .createSignedGetURL(properties.productImagesBucketName(), filename, Duration.ofMinutes(60))
                .toString();
    }
}

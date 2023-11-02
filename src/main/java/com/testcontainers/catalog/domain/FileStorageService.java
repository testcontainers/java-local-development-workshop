package com.testcontainers.catalog.domain;

import java.io.InputStream;

public interface FileStorageService {
    void createBucket(String bucketName);

    void upload(String filename, InputStream inputStream);

    String getPreSignedURL(String filename);
}

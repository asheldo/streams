package org.asheldo.streams.service;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

public class S3StreamServiceImpl implements S3StreamService {

    @Override
    public S3Client getS3Client(AwsCredentials credentials) {
        final StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);
        S3Client s3client = S3Client.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(credentialsProvider)
                .build();
        return s3client;
    }
}

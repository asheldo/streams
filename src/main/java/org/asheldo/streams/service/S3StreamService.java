package org.asheldo.streams.service;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.services.s3.S3Client;

public interface S3StreamService {
    S3Client getS3Client(AwsCredentials credentials);
}

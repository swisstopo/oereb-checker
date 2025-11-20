package ch.swisstopo.oerebchecker.aws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.regions.*;
import software.amazon.awssdk.services.s3.*;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.model.Bucket;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class S3Storage {

    private static final Logger logger = LoggerFactory.getLogger(S3Storage.class);

    private final S3Client client;
    private final String defaultBucketName;

    public S3Storage(String regionName, String bucketName) {

        client = S3Client.builder()
                .region(Region.of(regionName))
                .build();

        defaultBucketName = bucketName;
    }

    public S3Storage(String accessKey, String secretKey, String regionName, String bucketName) {

        AwsCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        client = S3Client.builder()
                .region(Region.of(regionName))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();

        defaultBucketName = bucketName;
    }

    public List<Bucket> GetBuckets() {

        List<Bucket> buckets = new ArrayList<>();
        try {
            buckets = client.listBuckets().buckets();

            logger.debug("Listing bucketNames:");
            for (Bucket b : buckets) {
                logger.debug("* {}", b.name());
            }
        } catch (S3Exception e) {
            log(e);
        }
        return buckets;
    }

    public List<S3Object> GetBucketObjects(String bucketName) {

        List<S3Object> objects = new ArrayList<>();
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();

            ListObjectsV2Response response = client.listObjectsV2(request);
            objects = response.contents();

            logger.debug("Listing objectKeys:");
            for (S3Object o : objects) {
                logger.debug("* {}", o.key());
            }
        } catch (S3Exception e) {
            log(e);
        }
        return objects;
    }

    public byte[] GetBucketObject(String bucketName, String objectKey) {

        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            ResponseBytes<GetObjectResponse> responseBytes = client.getObjectAsBytes(request);
            return responseBytes.asByteArray();

        } catch (S3Exception e) {
            log(e);
            return null;
        }
    }

    public byte[] GetBucketObject(String objectKey) {
        return GetBucketObject(defaultBucketName, objectKey);
    }

    public boolean PutBucketObject(String bucketName, Path filePath) {
        try {
            client.putObject(request -> request.bucket(bucketName).key(filePath.getFileName().toString()), filePath);
        } catch (S3Exception e) {
            log(e);
            return false;
        }
        return true;
    }

    public boolean PutBucketObject(Path filePath) {
        return PutBucketObject(defaultBucketName, filePath);
    }

    private void log(S3Exception e) {
        if (logger.isDebugEnabled()) {
            logger.debug("Failed to retrieve object: {} - Error code: {}",
                    e.awsErrorDetails().errorMessage(),
                    e.awsErrorDetails().errorCode(),
                    e
            );
        } else {
            logger.error("Failed to retrieve object: {} - Error code: {}",
                    e.awsErrorDetails().errorMessage(),
                    e.awsErrorDetails().errorCode()
            );
        }
    }
}

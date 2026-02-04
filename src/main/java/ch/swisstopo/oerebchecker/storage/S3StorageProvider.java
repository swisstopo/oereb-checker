package ch.swisstopo.oerebchecker.storage;

import ch.swisstopo.oerebchecker.utils.EnvVars;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

public class S3StorageProvider implements IStorageProvider {
    private static final Logger logger = LoggerFactory.getLogger(S3StorageProvider.class);

    private final S3Client client;
    private final String bucketName;

    private S3StorageProvider(S3Client client, String bucketName) {
        this.client = client;
        this.bucketName = bucketName;
    }

    public static Optional<S3StorageProvider> createFromEnv(String bucketEnvKey) {
        try {
            String regionName = System.getenv(EnvVars.S3_REGION_NAME);
            String bucketName = System.getenv(bucketEnvKey);

            if (regionName == null || bucketName == null) {
                logger.trace("S3 configuration missing value for {} or {}", EnvVars.S3_REGION_NAME, bucketEnvKey);
                return Optional.empty();
            }

            var builder = S3Client.builder().region(Region.of(regionName));

            String accessKey = System.getenv(EnvVars.S3_ACCESS_KEY);
            String secretKey = System.getenv(EnvVars.S3_SECRET_KEY);

            if (accessKey != null && secretKey != null) {
                builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
                logger.info("Using static credentials for S3.");
            } else {
                builder.credentialsProvider(DefaultCredentialsProvider.builder().build());
                logger.info("Using DefaultCredentialsProvider (IAM Roles/Profile) for S3.");
            }

            return Optional.of(new S3StorageProvider(builder.build(), bucketName));

        } catch (Exception e) {
            logger.error("Failed to initialize S3 provider", e);
            return Optional.empty();
        }
    }

    @Override
    public byte[] readObject(Path filePath) {
        String s3Key = normalizeS3Key(filePath);
        logger.trace("Reading object from S3: bucket={}, key={}", bucketName, s3Key);

        try {
            return client.getObjectAsBytes(b -> b.bucket(bucketName).key(s3Key)).asByteArray();
        } catch (S3Exception e) {
            logger.error("Access denied or object missing in S3 bucket '{}': {}", bucketName, s3Key);
            return null;
        }
    }

    @Override
    public boolean writeObject(Path filePath, InputStream inputStream) {
        String s3Key = normalizeS3Key(filePath);
        // Explicitly set Content-Type so CloudFront/Browsers render the file instead of downloading.
        final String contentType = determineContentType(filePath);

        try {
            client.putObject(b -> b.bucket(bucketName).key(s3Key).contentType(contentType), RequestBody.fromInputStream(inputStream, inputStream.available()));
            return true;
        } catch (Exception e) {
            logger.error("S3 Put failed: {}/{}", bucketName, s3Key, e);
            return false;
        }
    }

    private String determineContentType(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".html")) {
            return "text/html";
        } else if (fileName.endsWith(".json")) {
            return "application/json";
        } else if (fileName.endsWith(".css")) {
            return "text/css";
        }
        return "application/octet-stream";
    }

    @Override
    public boolean exists(Path filePath) {
        try {
            String s3Key = normalizeS3Key(filePath);
            client.headObject(b -> b.bucket(bucketName).key(s3Key));
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            logger.debug(e.getMessage(), e);
            return false;
        }
    }

    private String normalizeS3Key(Path path) {
        String original = path.toString();

        // 1. Convert to String and use forward slashes
        String key = original.replace("\\", "/");

        // 2. Remove any double slashes
        while (key.contains("//")) {
            key = key.replace("//", "/");
        }

        // 3. IMPORTANT: Remove leading slash if it exists
        // S3 keys should not start with a /
        if (key.startsWith("/")) {
            key = key.substring(1);
        }

        logger.trace("Normalized Path '{}' to S3 Key '{}'", original, key);
        return key;
    }
}

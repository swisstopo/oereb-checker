package ch.swisstopo.oerebchecker.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class LocalStorageProvider implements IStorageProvider {
    private static final Logger logger = LoggerFactory.getLogger(LocalStorageProvider.class);

    @Override
    public byte[] readObject(Path filePath) {
        try {
            if (!Files.exists(filePath)) {
                return null;
            }
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public boolean writeObject(Path filePath, InputStream inputStream) {
        try {
            // Ensure parent directories exist. Null check is crucial when writing directly
            // to a root path (e.g., Paths.get("result.html")).
            if (filePath.getParent() != null) {
                Files.createDirectories(filePath.getParent());
            }
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            logger.error("Failed to write local file: {}", filePath, e);
            return false;
        }
    }

    @Override
    public boolean exists(Path filePath) {
        return Files.exists(filePath);
    }

    @Override
    public List<Path> listObjects(String prefix) {
        Path dir = Paths.get(prefix);
        if (!Files.isDirectory(dir)) {
            return Collections.emptyList();
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream.filter(Files::isRegularFile).toList();
        } catch (IOException e) {
            logger.error("Failed to list local directory: {}", prefix, e);
            return Collections.emptyList();
        }
    }
}

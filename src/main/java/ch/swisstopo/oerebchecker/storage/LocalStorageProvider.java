package ch.swisstopo.oerebchecker.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.file.*;

public class LocalStorageProvider implements IStorageProvider {
    private static final Logger logger = LoggerFactory.getLogger(LocalStorageProvider.class);

    @Override
    public byte[] readObject(Path filePath) {
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public boolean writeObject(Path filePath, InputStream inputStream) {
        try {
            Files.createDirectories(filePath.getParent());
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
}

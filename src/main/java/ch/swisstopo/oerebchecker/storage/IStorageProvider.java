package ch.swisstopo.oerebchecker.storage;

import java.io.InputStream;
import java.nio.file.Path;

public interface IStorageProvider {

    byte[] readObject(Path filePath);

    boolean writeObject(Path filePath, InputStream inputStream);

    boolean exists(Path filePath);
}

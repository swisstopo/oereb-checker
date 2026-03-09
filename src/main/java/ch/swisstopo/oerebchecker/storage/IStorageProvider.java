package ch.swisstopo.oerebchecker.storage;

import java.io.InputStream;
import java.nio.file.Path;

public interface IStorageProvider {

    byte[] readObject(Path filePath);

    boolean writeObject(Path filePath, InputStream inputStream);

    boolean exists(Path filePath);

    /**
     * Lists all object keys (relative paths) under the given prefix.
     *
     * @param prefix the path prefix to list under (e.g. "data/")
     * @return list of matching paths, empty list if none found
     */
    java.util.List<Path> listObjects(String prefix);
}

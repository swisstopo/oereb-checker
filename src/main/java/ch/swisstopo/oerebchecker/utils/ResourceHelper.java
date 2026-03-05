package ch.swisstopo.oerebchecker.utils;

import java.io.*;
import java.net.URL;

public class ResourceHelper {

    public static URL getResourceUrl(String path) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(path);
        if (url == null) {
            url = ResourceHelper.class.getResource("/" + path);
        }
        return url;
    }

    public static InputStream getResourceStream(String path) {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (is == null) {
            is = ResourceHelper.class.getResourceAsStream("/" + path);
        }
        return is;
    }

    public static byte[] readResourceAsBytes(String path) throws IOException {
        try (InputStream is = getResourceStream(path)) {
            if (is == null) throw new FileNotFoundException("Resource not found: " + path);
            return is.readAllBytes();
        }
    }
}

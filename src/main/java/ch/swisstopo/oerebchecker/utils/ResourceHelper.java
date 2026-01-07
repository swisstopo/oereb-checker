package ch.swisstopo.oerebchecker.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ResourceHelper {

    public static InputStream getResourceStream(String path) {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (is == null) {
            is = ResourceHelper.class.getResourceAsStream("/" + path);
        }
        return is;
    }

    public static String readResourceAsString(String path) throws IOException {
        try (InputStream is = getResourceStream(path)) {
            if (is == null) throw new FileNotFoundException("Resource not found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static byte[] readResourceAsBytes(String path) throws IOException {
        try (InputStream is = getResourceStream(path)) {
            if (is == null) throw new FileNotFoundException("Resource not found: " + path);
            return is.readAllBytes();
        }
    }
}

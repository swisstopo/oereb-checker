package ch.swisstopo.oerebchecker.utils;

import ch.swisstopo.oerebchecker.core.validation.Validator;
import ch.swisstopo.oerebchecker.core.validation.ValidatorResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

class ValidatorTest {
    protected static final Logger logger = LoggerFactory.getLogger(ValidatorTest.class);

    @org.junit.jupiter.api.Test
    void checkXml() {

        try {
            Path directoryPath = Paths.get("src/test/resources/extracts/xmls").toAbsolutePath();
            File directory = directoryPath.toFile();
            if (directory.exists()) {
                for (File file : Objects.requireNonNull(directory.listFiles())) {
                    if (!file.isDirectory()) {
                        ValidatorResult result = Validator.checkXml(new FileInputStream(file));
                        if (result.IsValid != null && result.IsValid) {
                            logger.info("XML valid - '{}'", file.getName());
                        } else {
                            logger.warn("XML invalid - '{}'", file.getName());
                            result.Messages.forEach(e -> logger.debug("   {}", e.toString()));
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @org.junit.jupiter.api.Test
    void checkPdf() {
        try {
            Path directoryPath = Paths.get("src/test/resources/extracts/pdfs").toAbsolutePath();
            File directory = directoryPath.toFile();
            if (directory.exists()) {
                for (File file : Objects.requireNonNull(directory.listFiles())) {
                    if (!file.isDirectory()) {
                        ValidatorResult result = Validator.checkPdf(new FileInputStream(file));
                        if (result.IsValid != null && result.IsValid) {
                            logger.info("PDF valid - '{}'", file.getName());
                        } else {
                            logger.warn("PDF invalid - '{}'", file.getName());
                            result.Messages.forEach(e -> logger.info("   {}", e.toString()));
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
package ch.swisstopo.oerebchecker.utils;

import ch.swisstopo.oerebchecker.core.validation.Validator;
import ch.swisstopo.oerebchecker.core.validation.ValidatorResult;
import ch.swisstopo.oerebchecker.manager.UidManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

class UidManagerTest {
    protected static final Logger logger = LoggerFactory.getLogger(UidManagerTest.class);

    @org.junit.jupiter.api.Test
    void checkUid() {

        try {
            UidManager.validateUID("CHE-116.068.369");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
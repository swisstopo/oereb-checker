package ch.swisstopo.oerebchecker.manager;

import ch.swisstopo.oerebchecker.utils.uid.IPublicServices;
import ch.swisstopo.oerebchecker.utils.uid.PublicServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UidManager {
    private static final Logger logger = LoggerFactory.getLogger(UidManager.class);

    private static final Map<String, Boolean> validationCache = new ConcurrentHashMap<>();
    private static IPublicServices servicePort;

    static {
        try {
            // Load the WSDL via the ClassLoader to ensure it is found within the JAR/Docker
            // container.
            // Using absolute filesystem paths fails in the Lambda environment.
            java.net.URL wsdlResource = UidManager.class.getClassLoader()
                    .getResource("ch/swisstopo/bfs/www.uid-wse.admin.ch_V5.0_PublicServices.svc_wsdl.xml");
            if (wsdlResource == null) {
                throw new RuntimeException("UID Service WSDL not found in classpath");
            }
            PublicServices service = new PublicServices(wsdlResource);
            servicePort = service.getBasicHttpBindingIPublicServices();
            logger.info("UID Public Services initialized.");
        } catch (Exception e) {
            logger.error("Failed to initialize UID Public Services: {}", e.getMessage());
        }
    }

    public static boolean validateUID(String uid) {
        if (uid == null || uid.isBlank()) {
            return false;
        }

        return validationCache.computeIfAbsent(uid, key -> {
            try {
                logger.trace("Calling BFS UID Service for validation of: {}", key);
                return servicePort.validateUID(key);
            } catch (Exception e) {
                logger.warn("UID validation service failed for {}: {}", key, e.getMessage());
                return false;
            }
        });
    }
}
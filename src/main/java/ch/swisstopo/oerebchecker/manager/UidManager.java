package ch.swisstopo.oerebchecker.manager;

import ch.swisstopo.oerebchecker.utils.EnvVars;
import ch.swisstopo.oerebchecker.utils.ResourceHelper;
import ch.swisstopo.oerebchecker.utils.uid.*;
import jakarta.xml.ws.BindingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.utils.StringUtils;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UidManager {
    private static final Logger logger = LoggerFactory.getLogger(UidManager.class);

    private static final Map<String, Boolean> validationCache = new ConcurrentHashMap<>();

    private static IPartnerServices partnerServices;
    private static IPublicServices publicServices;

    static {

        try {
            String login = System.getenv(EnvVars.BFS_UID_PARTNER_LOGIN);
            String password = System.getenv(EnvVars.BFS_UID_PARTNER_PASSWORD);

            if (StringUtils.isBlank(login) || StringUtils.isBlank(password)) {
                logger.trace("No UID Partner Services credentials found in environment variables.");
            } else {
                // Load the WSDL via the ClassLoader to ensure it is found within the JAR/Docker container.
                java.net.URL wsdlResource = ResourceHelper.getResourceUrl("ch/swisstopo/bfs/Prod_PartnerServices.svc.wsdl");
                if (wsdlResource == null) {
                    throw new RuntimeException("BFS UID Partner Service WSDL not found in classpath");
                }
                partnerServices = new PartnerServices(wsdlResource).getBasicHttpBindingIPartnerServices();

                BindingProvider bp = (BindingProvider) partnerServices;
                bp.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, login);
                bp.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, password);

                logger.info("BFS UID Partner Services initialized.");
            }
        } catch (Exception e) {
            partnerServices = null;
            logger.error("Failed to initialize BFS UID Partner Services: {}", e.getMessage());
        }

        if (partnerServices == null) {
            try {
                // Load the WSDL via the ClassLoader to ensure it is found within the JAR/Docker container.
                java.net.URL wsdlResource = ResourceHelper.getResourceUrl("ch/swisstopo/bfs/Prod_PublicServices.svc.wsdl");
                if (wsdlResource == null) {
                    throw new RuntimeException("BFS UID Public Service WSDL not found in classpath");
                }
                publicServices = new PublicServices(wsdlResource).getBasicHttpBindingIPublicServices();
                logger.info("BFS UID Public Services initialized.");
            } catch (Exception e) {
                logger.error("Failed to initialize BFS UID Public Services: {}", e.getMessage());
            }
        }
    }

    public static boolean validateUID(String uid) {
        if (uid == null || uid.isBlank()) {
            return false;
        }

        return validationCache.computeIfAbsent(uid, key -> {
            try {

                if (partnerServices == null) {
                    logger.trace("Calling BFS UID public service for validation of: {}", key);
                    return publicServices.validateUID(key);
                } else {
                    logger.trace("Calling BFS UID partner service for validation of: {}", key);
                    return partnerValidateUID(key);
                }
            } catch (Exception e) {
                logger.warn("BFS UID validation {} service failed for {}: {}", partnerServices == null ? "public" : "partner", key, e.getMessage());
                return false;
            }
        });
    }

    private static boolean partnerValidateUID(String originalUid) {
        if (originalUid == null || originalUid.isBlank()) {
            return false;
        }

        try {
            String uid = originalUid.replace("-", "").replace(".", "");
            UidOrganisationIdCategorieType uidCategory = UidOrganisationIdCategorieType.fromValue(uid.substring(0, 3).toUpperCase());
            BigInteger uidNumber = BigInteger.valueOf(Long.parseLong(uid.substring(3)));

            logger.trace("UID category: {}, UID number: {}", uidCategory, uidNumber);

            UidStructureType uidStructureType = new UidStructureType();
            uidStructureType.setUidOrganisationIdCategorie(uidCategory);
            uidStructureType.setUidOrganisationId(uidNumber);

            UidEntitySearchRequest uidEntitySearchRequest = new UidEntitySearchRequest();
            uidEntitySearchRequest.setUid(uidStructureType);

            SearchConfiguration searchConfiguration = new SearchConfiguration();

            var result = partnerServices.quickSearch(uidEntitySearchRequest, searchConfiguration);
            return !result.getUidEntitySearchResultItem().isEmpty();

        } catch (Exception e) {
            logger.warn("UID validation partner service failed for {}: {}", originalUid, e.getMessage());
            return false;
        }
    }
}
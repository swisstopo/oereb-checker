package ch.swisstopo.oerebchecker.manager;

import ch.swisstopo.oereb.V20Gesetze;
import ch.swisstopo.oereb.V20Texte;
import ch.swisstopo.oereb.V20Themen;
import ch.swisstopo.oerebchecker.models.FederalTopicInformation;
import ch.swisstopo.oerebchecker.utils.ResourceHelper;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetadataManager {
    private static final Logger logger = LoggerFactory.getLogger(MetadataManager.class);

    private static boolean metadataInitialized = false;
    private static final Map<String, FederalTopicInformation> federalTopicInformation = new HashMap<>();
    private static V20Texte.DATASECTION.V20Konfiguration v20Konfiguration;

    private static <T> T unmarshal(String resourcePath, Class<T> clazz) throws Exception {
        try (InputStream is = ResourceHelper.getResourceStream(resourcePath)) {
            if (is == null) {
                throw new FileNotFoundException("Critical metadata resource not found: " + resourcePath);
            }
            try (BufferedInputStream bis = new BufferedInputStream(is)) { // Buffer the stream

                JAXBContext context = JAXBContext.newInstance(clazz);
                Unmarshaller unmarshaller = context.createUnmarshaller();
                return clazz.cast(unmarshaller.unmarshal(bis));
            }
        }
    }

    public static synchronized void loadMetadata() {
        if (metadataInitialized) {
            return;
        }
        try {
            V20Themen themes = unmarshal("ch/swisstopo/oereb/OeREBKRM_V2_0_Themen.xml", V20Themen.class);
            V20Gesetze laws = unmarshal("ch/swisstopo/oereb/OeREBKRM_V2_0_Gesetze.xml", V20Gesetze.class);
            List<V20Gesetze.DATASECTION.V20Dokumente.V20DokumenteDokument> docs = laws.getDATASECTION().getV20Dokumente().getV20DokumenteDokument();

            // Process Federal Topic Info
            for (Object obj : themes.getDATASECTION().getV20Thema().getV20ThemaThemaOrV20ThemaThemaGesetz()) {
                if (obj instanceof V20Themen.DATASECTION.V20Thema.V20ThemaThema thema) {
                    federalTopicInformation.put(thema.getCode(), new FederalTopicInformation(thema));
                }
            }

            // Link Laws to Themes
            for (Object object : themes.getDATASECTION().getV20Thema().getV20ThemaThemaOrV20ThemaThemaGesetz()) {
                if (object instanceof V20Themen.DATASECTION.V20Thema.V20ThemaThemaGesetz themaGesetz) {
                    String themaRef = themaGesetz.getThema().getREF();
                    String gesetzRef = themaGesetz.getGesetz().getREF();

                    docs.stream()
                            .filter(d -> gesetzRef.equals(d.getTID()))
                            .findFirst()
                            .ifPresent(gesetzDokument -> {
                                FederalTopicInformation info = federalTopicInformation.get(themaRef);
                                if (info != null) {
                                    info.addDokument(gesetzDokument);
                                } else {
                                    logger.error("Failed to load OeREB KRM: Federal topic information does not contain theme reference: {}", themaRef);
                                }
                            });
                }
            }

            // Load Config Texts
            V20Texte texte = unmarshal("ch/swisstopo/oereb/OeREBKRM_V2_0_Texte.xml", V20Texte.class);
            v20Konfiguration = texte.getDATASECTION().getV20Konfiguration();

            metadataInitialized = true;
        } catch (Exception e) {
            logger.error("Failed to load OeREB KRM metadata. This is a fatal error.", e);
            throw new RuntimeException("Critical metadata missing", e);
        }
    }

    public static Map<String, FederalTopicInformation> getFederalTopicInformation() {
        if (!metadataInitialized) loadMetadata();
        return federalTopicInformation;
    }

    public static V20Texte.DATASECTION.V20Konfiguration getV20Konfiguration() {
        if (!metadataInitialized) loadMetadata();
        return v20Konfiguration;
    }
}
package ch.swisstopo.oereb;

import jakarta.xml.bind.annotation.*;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "headersection",
        "datasection"
})
@XmlRootElement(name = "TRANSFER", namespace = "http://www.interlis.ch/INTERLIS2.3")
public class V20Gesetze {

    @XmlElement(name = "HEADERSECTION", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
    protected V20Gesetze.HEADERSECTION headersection;
    @XmlElement(name = "DATASECTION", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
    protected V20Gesetze.DATASECTION datasection;

    public V20Gesetze.HEADERSECTION getHEADERSECTION() {
        return headersection;
    }

    public V20Gesetze.DATASECTION getDATASECTION() {
        return datasection;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class HEADERSECTION {
        @XmlElement(name = "MODELS", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
        protected MODELS models;
        @XmlAttribute(name = "SENDER", required = true)
        protected String sender;
        @XmlAttribute(name = "VERSION", required = true)
        protected BigDecimal version;

        @XmlAccessorType(XmlAccessType.FIELD)
        public static class MODELS {
            @XmlElement(name = "MODEL", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
            protected List<MODEL> model;

            @XmlAccessorType(XmlAccessType.FIELD)
            public static class MODEL {
                @XmlAttribute(name = "NAME")
                protected String name;
                @XmlAttribute(name = "VERSION")
                protected String version;
            }
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class DATASECTION {
        @XmlElement(name = "OeREBKRM_V2_0.Dokumente", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
        protected V20Dokumente v20Dokumente;

        public V20Dokumente getV20Dokumente() {
            return v20Dokumente;
        }

        @XmlAccessorType(XmlAccessType.FIELD)
        public static class V20Dokumente {
            @XmlElement(name = "OeREBKRM_V2_0.Dokumente.Dokument", namespace = "http://www.interlis.ch/INTERLIS2.3")
            protected List<V20DokumenteDokument> v20DokumenteDokument;
            @XmlElement(name = "OeREBKRM_V2_0.Amt.Amt", namespace = "http://www.interlis.ch/INTERLIS2.3")
            protected List<V20AmtAmt> v20AmtAmt;
            @XmlAttribute(name = "BID")
            protected String bid;

            public List<V20DokumenteDokument> getV20DokumenteDokument() {
                if (v20DokumenteDokument == null) v20DokumenteDokument = new ArrayList<>();
                return v20DokumenteDokument;
            }

            @XmlAccessorType(XmlAccessType.FIELD)
            public static class V20DokumenteDokument {
                @XmlElement(name = "Typ", namespace = "http://www.interlis.ch/INTERLIS2.3")
                protected String typ;
                @XmlElement(name = "Titel", namespace = "http://www.interlis.ch/INTERLIS2.3")
                protected MultilingualText titel;
                @XmlElement(name = "Abkuerzung", namespace = "http://www.interlis.ch/INTERLIS2.3")
                protected MultilingualText abkuerzung;
                @XmlElement(name = "OffizielleNr", namespace = "http://www.interlis.ch/INTERLIS2.3")
                protected MultilingualText offizielleNr;
                @XmlElement(name = "TextImWeb", namespace = "http://www.interlis.ch/INTERLIS2.3")
                protected MultilingualUri textImWeb;
                @XmlElement(name = "PubliziertAb", namespace = "http://www.interlis.ch/INTERLIS2.3")
                protected XMLGregorianCalendar publiziertAb;
                @XmlElement(name = "ZustaendigeStelle", namespace = "http://www.interlis.ch/INTERLIS2.3")
                protected ZustaendigeStelle zustaendigeStelle;
                @XmlAttribute(name = "TID")
                protected String tid;

                public String getTyp() {
                    return typ;
                }

                public MultilingualText getTitel() {
                    return titel;
                }

                public MultilingualText getAbkuerzung() {
                    return abkuerzung;
                }

                public MultilingualText getOffizielleNr() {
                    return offizielleNr;
                }

                public MultilingualUri getTextImWeb() {
                    return textImWeb;
                }

                public String getTID() {
                    return tid;
                }

                @XmlAccessorType(XmlAccessType.FIELD)
                public static class MultilingualText {
                    @XmlElement(name = "LocalisationCH_V1.MultilingualText", namespace = "http://www.interlis.ch/INTERLIS2.3")
                    protected LocalisationTextContainer localisationCHV1MultilingualText;

                    public LocalisationTextContainer getLocalisationCHV1MultilingualText() {
                        return localisationCHV1MultilingualText;
                    }
                }

                @XmlAccessorType(XmlAccessType.FIELD)
                public static class MultilingualUri {
                    @XmlElement(name = "OeREBKRM_V2_0.MultilingualUri", namespace = "http://www.interlis.ch/INTERLIS2.3")
                    protected LocalisedUriContainer localisedUriContainer;

                    public LocalisedUriContainer getLocalisationCHV1MultilingualUri() {
                        return localisedUriContainer;
                    }
                }

                @XmlAccessorType(XmlAccessType.FIELD)
                public static class ZustaendigeStelle {
                    @XmlAttribute(name = "REF")
                    protected String ref;
                }
            }

            @XmlAccessorType(XmlAccessType.FIELD)
            public static class V20AmtAmt {
                @XmlElement(name = "Name", namespace = "http://www.interlis.ch/INTERLIS2.3")
                protected V20DokumenteDokument.MultilingualText name;
                @XmlAttribute(name = "TID")
                protected String tid;
            }
        }
    }

    // Helper classes for Multilingual structures
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class LocalisationTextContainer {
        @XmlElement(name = "LocalisedText", namespace = "http://www.interlis.ch/INTERLIS2.3")
        protected List<LocalisedTextWrapper> localisedText;

        public List<LocalisedTextWrapper> getLocalisedText() {
            return localisedText;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class LocalisedTextWrapper {
        @XmlElement(name = "LocalisationCH_V1.LocalisedText", namespace = "http://www.interlis.ch/INTERLIS2.3")
        protected LocalisedTextContent localisationCHV1LocalisedText;

        public LocalisedTextContent getLocalisationCHV1LocalisedText() {
            return localisationCHV1LocalisedText;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class LocalisedTextContent {
        @XmlElement(name = "Language", namespace = "http://www.interlis.ch/INTERLIS2.3")
        protected String language;
        @XmlElement(name = "Text", namespace = "http://www.interlis.ch/INTERLIS2.3")
        protected String text;

        public String getLanguage() {
            return language;
        }

        public String getText() {
            return text;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class LocalisedUriContainer {
        @XmlElement(name = "LocalisedText", namespace = "http://www.interlis.ch/INTERLIS2.3")
        protected List<LocalisedUriWrapper> localisedText;

        public List<LocalisedUriWrapper> getLocalisedUri() {
            return localisedText;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class LocalisedUriWrapper {
        @XmlElement(name = "OeREBKRM_V2_0.LocalisedUri", namespace = "http://www.interlis.ch/INTERLIS2.3")
        protected LocalisedTextContent localisedUri;

        public LocalisedTextContent getOeREBKRMV20LocalisedUri() {
            return localisedUri;
        }
    }
}
package ch.swisstopo.oereb;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "headersection",
        "datasection"
})
@XmlRootElement(name = "TRANSFER", namespace = "http://www.interlis.ch/INTERLIS2.3")
public class V20Texte {

    @XmlElement(name = "HEADERSECTION", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
    protected V20Texte.HEADERSECTION headersection;
    @XmlElement(name = "DATASECTION", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
    protected V20Texte.DATASECTION datasection;

    public V20Texte.HEADERSECTION getHEADERSECTION() {
        return headersection;
    }

    public void setHEADERSECTION(V20Texte.HEADERSECTION value) {
        this.headersection = value;
    }

    public V20Texte.DATASECTION getDATASECTION() {
        return datasection;
    }

    public void setDATASECTION(V20Texte.DATASECTION value) {
        this.datasection = value;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
            "v20Konfiguration"
    })
    public static class DATASECTION {

        @XmlElement(name = "OeREBKRMkvs_V2_0.Konfiguration", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
        protected V20Konfiguration v20Konfiguration;

        public V20Konfiguration getV20Konfiguration() {
            return v20Konfiguration;
        }

        public void setV20Konfiguration(V20Konfiguration value) {
            this.v20Konfiguration = value;
        }

        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
                "v20KonfigurationRechtsStatusTxt",
                "v20KonfigurationDokumentTypTxt",
                "v20KonfigurationGrundstuecksArtTxt",
                "v20KonfigurationGlossar",
                "v20KonfigurationHaftungshinweis",
                "v20KonfigurationInformation"
        })
        public static class V20Konfiguration {

            @XmlElement(name = "OeREBKRMkvs_V2_0.Konfiguration.RechtsStatusTxt", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
            protected List<V20KonfigurationRechtsStatusTxt> v20KonfigurationRechtsStatusTxt;
            @XmlElement(name = "OeREBKRMkvs_V2_0.Konfiguration.DokumentTypTxt", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
            protected List<V20KonfigurationDokumentTypTxt> v20KonfigurationDokumentTypTxt;
            @XmlElement(name = "OeREBKRMkvs_V2_0.Konfiguration.GrundstuecksArtTxt", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
            protected List<V20KonfigurationGrundstuecksArtTxt> v20KonfigurationGrundstuecksArtTxt;
            @XmlElement(name = "OeREBKRMkvs_V2_0.Konfiguration.Glossar", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
            protected List<V20KonfigurationGlossar> v20KonfigurationGlossar;
            @XmlElement(name = "OeREBKRMkvs_V2_0.Konfiguration.Haftungshinweis", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
            protected List<V20KonfigurationHaftungshinweis> v20KonfigurationHaftungshinweis;
            @XmlElement(name = "OeREBKRMkvs_V2_0.Konfiguration.Information", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
            protected V20KonfigurationInformation v20KonfigurationInformation;
            @XmlAttribute(name = "BID", required = true)
            protected String bid;


            public List<V20KonfigurationRechtsStatusTxt> getV20KonfigurationRechtsStatusTxt() {
                if (v20KonfigurationRechtsStatusTxt == null) {
                    v20KonfigurationRechtsStatusTxt = new ArrayList<V20KonfigurationRechtsStatusTxt>();
                }
                return this.v20KonfigurationRechtsStatusTxt;
            }

            public List<V20KonfigurationDokumentTypTxt> getV20KonfigurationDokumentTypTxt() {
                if (v20KonfigurationDokumentTypTxt == null) {
                    v20KonfigurationDokumentTypTxt = new ArrayList<V20KonfigurationDokumentTypTxt>();
                }
                return this.v20KonfigurationDokumentTypTxt;
            }

            public List<V20KonfigurationGrundstuecksArtTxt> getV20KonfigurationGrundstuecksArtTxt() {
                if (v20KonfigurationGrundstuecksArtTxt == null) {
                    v20KonfigurationGrundstuecksArtTxt = new ArrayList<V20KonfigurationGrundstuecksArtTxt>();
                }
                return this.v20KonfigurationGrundstuecksArtTxt;
            }

            public List<V20KonfigurationGlossar> getV20KonfigurationGlossar() {
                if (v20KonfigurationGlossar == null) {
                    v20KonfigurationGlossar = new ArrayList<V20KonfigurationGlossar>();
                }
                return this.v20KonfigurationGlossar;
            }

            public List<V20KonfigurationHaftungshinweis> getV20KonfigurationHaftungshinweis() {
                if (v20KonfigurationHaftungshinweis == null) {
                    v20KonfigurationHaftungshinweis = new ArrayList<V20KonfigurationHaftungshinweis>();
                }
                return this.v20KonfigurationHaftungshinweis;
            }

            public V20KonfigurationInformation getV20KonfigurationInformation() {
                return v20KonfigurationInformation;
            }

            public void setV20KonfigurationInformation(V20KonfigurationInformation value) {
                this.v20KonfigurationInformation = value;
            }

            public String getBID() {
                return bid;
            }

            public void setBID(String value) {
                this.bid = value;
            }

            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {
                    "code",
                    "titel"
            })
            public static class V20KonfigurationDokumentTypTxt {

                @XmlElement(name = "Code", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                protected String code;
                @XmlElement(name = "Titel", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                protected V20KonfigurationDokumentTypTxt.Titel titel;
                @XmlAttribute(name = "TID", required = true)
                @XmlSchemaType(name = "unsignedByte")
                protected short tid;

                public String getCode() {
                    return code;
                }

                public void setCode(String value) {
                    this.code = value;
                }

                public V20KonfigurationDokumentTypTxt.Titel getTitel() {
                    return titel;
                }

                public void setTitel(V20KonfigurationDokumentTypTxt.Titel value) {
                    this.titel = value;
                }

                public short getTID() {
                    return tid;
                }

                public void setTID(short value) {
                    this.tid = value;
                }

                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "", propOrder = {
                        "localisationCHV1MultilingualText"
                })
                public static class Titel {

                    @XmlElement(name = "LocalisationCH_V1.MultilingualText", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                    protected V20KonfigurationDokumentTypTxt.Titel.LocalisationCHV1MultilingualText localisationCHV1MultilingualText;

                    public V20KonfigurationDokumentTypTxt.Titel.LocalisationCHV1MultilingualText getLocalisationCHV1MultilingualText() {
                        return localisationCHV1MultilingualText;
                    }

                    public void setLocalisationCHV1MultilingualText(V20KonfigurationDokumentTypTxt.Titel.LocalisationCHV1MultilingualText value) {
                        this.localisationCHV1MultilingualText = value;
                    }


                    @XmlAccessorType(XmlAccessType.FIELD)
                    @XmlType(name = "", propOrder = {
                            "localisedText"
                    })
                    public static class LocalisationCHV1MultilingualText {

                        @XmlElement(name = "LocalisedText", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                        protected V20KonfigurationDokumentTypTxt.Titel.LocalisationCHV1MultilingualText.LocalisedText localisedText;

                        public V20KonfigurationDokumentTypTxt.Titel.LocalisationCHV1MultilingualText.LocalisedText getLocalisedText() {
                            return localisedText;
                        }

                        public void setLocalisedText(V20KonfigurationDokumentTypTxt.Titel.LocalisationCHV1MultilingualText.LocalisedText value) {
                            this.localisedText = value;
                        }


                        @XmlAccessorType(XmlAccessType.FIELD)
                        @XmlType(name = "", propOrder = {
                                "localisationCHV1LocalisedText"
                        })
                        public static class LocalisedText {

                            @XmlElement(name = "LocalisationCH_V1.LocalisedText", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                            protected List<V20KonfigurationDokumentTypTxt.Titel.LocalisationCHV1MultilingualText.LocalisedText.LocalisationCHV1LocalisedText> localisationCHV1LocalisedText;

                            public List<V20KonfigurationDokumentTypTxt.Titel.LocalisationCHV1MultilingualText.LocalisedText.LocalisationCHV1LocalisedText> getLocalisationCHV1LocalisedText() {
                                if (localisationCHV1LocalisedText == null) {
                                    localisationCHV1LocalisedText = new ArrayList<V20KonfigurationDokumentTypTxt.Titel.LocalisationCHV1MultilingualText.LocalisedText.LocalisationCHV1LocalisedText>();
                                }
                                return this.localisationCHV1LocalisedText;
                            }


                            @XmlAccessorType(XmlAccessType.FIELD)
                            @XmlType(name = "", propOrder = {
                                    "language",
                                    "text"
                            })
                            public static class LocalisationCHV1LocalisedText {

                                @XmlElement(name = "Language", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                                protected String language;
                                @XmlElement(name = "Text", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                                protected String text;

                                public String getLanguage() {
                                    return language;
                                }

                                public void setLanguage(String value) {
                                    this.language = value;
                                }

                                public String getText() {
                                    return text;
                                }

                                public void setText(String value) {
                                    this.text = value;
                                }

                            }

                        }

                    }

                }

            }


            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {
                    "titel",
                    "inhalt"
            })
            public static class V20KonfigurationGlossar {

                @XmlElement(name = "Titel", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                protected V20KonfigurationGlossar.Titel titel;
                @XmlElement(name = "Inhalt", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                protected V20KonfigurationGlossar.Inhalt inhalt;
                @XmlAttribute(name = "TID", required = true)
                @XmlSchemaType(name = "unsignedByte")
                protected short tid;

                public V20KonfigurationGlossar.Titel getTitel() {
                    return titel;
                }

                public void setTitel(V20KonfigurationGlossar.Titel value) {
                    this.titel = value;
                }

                public V20KonfigurationGlossar.Inhalt getInhalt() {
                    return inhalt;
                }

                public void setInhalt(V20KonfigurationGlossar.Inhalt value) {
                    this.inhalt = value;
                }

                public short getTID() {
                    return tid;
                }

                public void setTID(short value) {
                    this.tid = value;
                }


                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "", propOrder = {
                        "localisationCHV1MultilingualMText"
                })
                public static class Inhalt {

                    @XmlElement(name = "LocalisationCH_V1.MultilingualMText", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                    protected V20KonfigurationGlossar.Inhalt.LocalisationCHV1MultilingualMText localisationCHV1MultilingualMText;

                    public V20KonfigurationGlossar.Inhalt.LocalisationCHV1MultilingualMText getLocalisationCHV1MultilingualMText() {
                        return localisationCHV1MultilingualMText;
                    }

                    public void setLocalisationCHV1MultilingualMText(V20KonfigurationGlossar.Inhalt.LocalisationCHV1MultilingualMText value) {
                        this.localisationCHV1MultilingualMText = value;
                    }


                    @XmlAccessorType(XmlAccessType.FIELD)
                    @XmlType(name = "", propOrder = {
                            "localisedText"
                    })
                    public static class LocalisationCHV1MultilingualMText {

                        @XmlElement(name = "LocalisedText", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                        protected V20KonfigurationGlossar.Inhalt.LocalisationCHV1MultilingualMText.LocalisedText localisedText;

                        public V20KonfigurationGlossar.Inhalt.LocalisationCHV1MultilingualMText.LocalisedText getLocalisedText() {
                            return localisedText;
                        }

                        public void setLocalisedText(V20KonfigurationGlossar.Inhalt.LocalisationCHV1MultilingualMText.LocalisedText value) {
                            this.localisedText = value;
                        }


                        @XmlAccessorType(XmlAccessType.FIELD)
                        @XmlType(name = "", propOrder = {
                                "localisationCHV1LocalisedMText"
                        })
                        public static class LocalisedText {

                            @XmlElement(name = "LocalisationCH_V1.LocalisedMText", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                            protected List<V20KonfigurationGlossar.Inhalt.LocalisationCHV1MultilingualMText.LocalisedText.LocalisationCHV1LocalisedMText> localisationCHV1LocalisedMText;

                            public List<V20KonfigurationGlossar.Inhalt.LocalisationCHV1MultilingualMText.LocalisedText.LocalisationCHV1LocalisedMText> getLocalisationCHV1LocalisedMText() {
                                if (localisationCHV1LocalisedMText == null) {
                                    localisationCHV1LocalisedMText = new ArrayList<V20KonfigurationGlossar.Inhalt.LocalisationCHV1MultilingualMText.LocalisedText.LocalisationCHV1LocalisedMText>();
                                }
                                return this.localisationCHV1LocalisedMText;
                            }


                            @XmlAccessorType(XmlAccessType.FIELD)
                            @XmlType(name = "", propOrder = {
                                    "language",
                                    "text"
                            })
                            public static class LocalisationCHV1LocalisedMText {

                                @XmlElement(name = "Language", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                                protected String language;
                                @XmlElement(name = "Text", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                                protected String text;

                                public String getLanguage() {
                                    return language;
                                }

                                public void setLanguage(String value) {
                                    this.language = value;
                                }

                                public String getText() {
                                    return text;
                                }

                                public void setText(String value) {
                                    this.text = value;
                                }

                            }

                        }

                    }

                }


                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "", propOrder = {
                        "localisationCHV1MultilingualText"
                })
                public static class Titel {

                    @XmlElement(name = "LocalisationCH_V1.MultilingualText", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                    protected V20KonfigurationGlossar.Titel.LocalisationCHV1MultilingualText localisationCHV1MultilingualText;

                    public V20KonfigurationGlossar.Titel.LocalisationCHV1MultilingualText getLocalisationCHV1MultilingualText() {
                        return localisationCHV1MultilingualText;
                    }

                    public void setLocalisationCHV1MultilingualText(V20KonfigurationGlossar.Titel.LocalisationCHV1MultilingualText value) {
                        this.localisationCHV1MultilingualText = value;
                    }


                    @XmlAccessorType(XmlAccessType.FIELD)
                    @XmlType(name = "", propOrder = {
                            "localisedText"
                    })
                    public static class LocalisationCHV1MultilingualText {

                        @XmlElement(name = "LocalisedText", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                        protected V20KonfigurationGlossar.Titel.LocalisationCHV1MultilingualText.LocalisedText localisedText;

                        public V20KonfigurationGlossar.Titel.LocalisationCHV1MultilingualText.LocalisedText getLocalisedText() {
                            return localisedText;
                        }

                        public void setLocalisedText(V20KonfigurationGlossar.Titel.LocalisationCHV1MultilingualText.LocalisedText value) {
                            this.localisedText = value;
                        }


                        @XmlAccessorType(XmlAccessType.FIELD)
                        @XmlType(name = "", propOrder = {
                                "localisationCHV1LocalisedText"
                        })
                        public static class LocalisedText {

                            @XmlElement(name = "LocalisationCH_V1.LocalisedText", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                            protected List<V20KonfigurationGlossar.Titel.LocalisationCHV1MultilingualText.LocalisedText.LocalisationCHV1LocalisedText> localisationCHV1LocalisedText;

                            public List<V20KonfigurationGlossar.Titel.LocalisationCHV1MultilingualText.LocalisedText.LocalisationCHV1LocalisedText> getLocalisationCHV1LocalisedText() {
                                if (localisationCHV1LocalisedText == null) {
                                    localisationCHV1LocalisedText = new ArrayList<V20KonfigurationGlossar.Titel.LocalisationCHV1MultilingualText.LocalisedText.LocalisationCHV1LocalisedText>();
                                }
                                return this.localisationCHV1LocalisedText;
                            }


                            @XmlAccessorType(XmlAccessType.FIELD)
                            @XmlType(name = "", propOrder = {
                                    "language",
                                    "text"
                            })
                            public static class LocalisationCHV1LocalisedText {

                                @XmlElement(name = "Language", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                                protected String language;
                                @XmlElement(name = "Text", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                                protected String text;

                                public String getLanguage() {
                                    return language;
                                }

                                public void setLanguage(String value) {
                                    this.language = value;
                                }

                                public String getText() {
                                    return text;
                                }

                                public void setText(String value) {
                                    this.text = value;
                                }

                            }

                        }

                    }

                }

            }


            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {
                    "code",
                    "titel"
            })
            public static class V20KonfigurationGrundstuecksArtTxt {

                @XmlElement(name = "Code", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                protected String code;
                @XmlElement(name = "Titel", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                protected V20KonfigurationGrundstuecksArtTxt.Titel titel;
                @XmlAttribute(name = "TID", required = true)
                @XmlSchemaType(name = "unsignedByte")
                protected short tid;

                public String getCode() {
                    return code;
                }

                public void setCode(String value) {
                    this.code = value;
                }

                public V20KonfigurationGrundstuecksArtTxt.Titel getTitel() {
                    return titel;
                }

                public void setTitel(V20KonfigurationGrundstuecksArtTxt.Titel value) {
                    this.titel = value;
                }

                public short getTID() {
                    return tid;
                }

                public void setTID(short value) {
                    this.tid = value;
                }


                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "", propOrder = {
                        "localisationCHV1MultilingualText"
                })
                public static class Titel {

                    @XmlElement(name = "LocalisationCH_V1.MultilingualText", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                    protected V20KonfigurationGrundstuecksArtTxt.Titel.LocalisationCHV1MultilingualText localisationCHV1MultilingualText;

                    public V20KonfigurationGrundstuecksArtTxt.Titel.LocalisationCHV1MultilingualText getLocalisationCHV1MultilingualText() {
                        return localisationCHV1MultilingualText;
                    }

                    public void setLocalisationCHV1MultilingualText(V20KonfigurationGrundstuecksArtTxt.Titel.LocalisationCHV1MultilingualText value) {
                        this.localisationCHV1MultilingualText = value;
                    }


                    @XmlAccessorType(XmlAccessType.FIELD)
                    @XmlType(name = "", propOrder = {
                            "localisedText"
                    })
                    public static class LocalisationCHV1MultilingualText {

                        @XmlElement(name = "LocalisedText", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                        protected V20KonfigurationGrundstuecksArtTxt.Titel.LocalisationCHV1MultilingualText.LocalisedText localisedText;

                        public V20KonfigurationGrundstuecksArtTxt.Titel.LocalisationCHV1MultilingualText.LocalisedText getLocalisedText() {
                            return localisedText;
                        }

                        public void setLocalisedText(V20KonfigurationGrundstuecksArtTxt.Titel.LocalisationCHV1MultilingualText.LocalisedText value) {
                            this.localisedText = value;
                        }


                        @XmlAccessorType(XmlAccessType.FIELD)
                        @XmlType(name = "", propOrder = {
                                "localisationCHV1LocalisedText"
                        })
                        public static class LocalisedText {

                            @XmlElement(name = "LocalisationCH_V1.LocalisedText", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                            protected List<V20KonfigurationGrundstuecksArtTxt.Titel.LocalisationCHV1MultilingualText.LocalisedText.LocalisationCHV1LocalisedText> localisationCHV1LocalisedText;

                            public List<V20KonfigurationGrundstuecksArtTxt.Titel.LocalisationCHV1MultilingualText.LocalisedText.LocalisationCHV1LocalisedText> getLocalisationCHV1LocalisedText() {
                                if (localisationCHV1LocalisedText == null) {
                                    localisationCHV1LocalisedText = new ArrayList<V20KonfigurationGrundstuecksArtTxt.Titel.LocalisationCHV1MultilingualText.LocalisedText.LocalisationCHV1LocalisedText>();
                                }
                                return this.localisationCHV1LocalisedText;
                            }


                            @XmlAccessorType(XmlAccessType.FIELD)
                            @XmlType(name = "", propOrder = {
                                    "language",
                                    "text"
                            })
                            public static class LocalisationCHV1LocalisedText {

                                @XmlElement(name = "Language", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                                protected String language;
                                @XmlElement(name = "Text", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                                protected String text;

                                public String getLanguage() {
                                    return language;
                                }

                                public void setLanguage(String value) {
                                    this.language = value;
                                }

                                public String getText() {
                                    return text;
                                }

                                public void setText(String value) {
                                    this.text = value;
                                }

                            }

                        }

                    }

                }

            }


            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {
                    "titel",
                    "inhalt",
                    "auszugIndex"
            })
            public static class V20KonfigurationHaftungshinweis {

                @XmlElement(name = "Titel", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                protected V20KonfigurationHaftungshinweis.Titel titel;
                @XmlElement(name = "Inhalt", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                protected V20KonfigurationHaftungshinweis.Inhalt inhalt;
                @XmlElement(name = "AuszugIndex", namespace = "http://www.interlis.ch/INTERLIS2.3")
                @XmlSchemaType(name = "unsignedByte")
                protected short auszugIndex;
                @XmlAttribute(name = "TID", required = true)
                protected BigDecimal tid;

                public V20KonfigurationHaftungshinweis.Titel getTitel() {
                    return titel;
                }

                public void setTitel(V20KonfigurationHaftungshinweis.Titel value) {
                    this.titel = value;
                }

                public V20KonfigurationHaftungshinweis.Inhalt getInhalt() {
                    return inhalt;
                }

                public void setInhalt(V20KonfigurationHaftungshinweis.Inhalt value) {
                    this.inhalt = value;
                }

                public short getAuszugIndex() {
                    return auszugIndex;
                }

                public void setAuszugIndex(short value) {
                    this.auszugIndex = value;
                }

                public BigDecimal getTID() {
                    return tid;
                }

                public void setTID(BigDecimal value) {
                    this.tid = value;
                }


                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "", propOrder = {
                        "localisationCHV1MultilingualMText"
                })
                public static class Inhalt {

                    @XmlElement(name = "LocalisationCH_V1.MultilingualMText", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                    protected V20KonfigurationHaftungshinweis.Inhalt.LocalisationCHV1MultilingualMText localisationCHV1MultilingualMText;

                    public V20KonfigurationHaftungshinweis.Inhalt.LocalisationCHV1MultilingualMText getLocalisationCHV1MultilingualMText() {
                        return localisationCHV1MultilingualMText;
                    }

                    public void setLocalisationCHV1MultilingualMText(V20KonfigurationHaftungshinweis.Inhalt.LocalisationCHV1MultilingualMText value) {
                        this.localisationCHV1MultilingualMText = value;
                    }


                    @XmlAccessorType(XmlAccessType.FIELD)
                    @XmlType(name = "", propOrder = {
                            "localisedText"
                    })
                    public static class LocalisationCHV1MultilingualMText {

                        @XmlElement(name = "LocalisedText", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                        protected V20KonfigurationHaftungshinweis.Inhalt.LocalisationCHV1MultilingualMText.LocalisedText localisedText;

                        public V20KonfigurationHaftungshinweis.Inhalt.LocalisationCHV1MultilingualMText.LocalisedText getLocalisedText() {
                            return localisedText;
                        }

                        public void setLocalisedText(V20KonfigurationHaftungshinweis.Inhalt.LocalisationCHV1MultilingualMText.LocalisedText value) {
                            this.localisedText = value;
                        }


                        @XmlAccessorType(XmlAccessType.FIELD)
                        @XmlType(name = "", propOrder = {
                                "localisationCHV1LocalisedMText"
                        })
                        public static class LocalisedText {

                            @XmlElement(name = "LocalisationCH_V1.LocalisedMText", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                            protected List<V20KonfigurationHaftungshinweis.Inhalt.LocalisationCHV1MultilingualMText.LocalisedText.LocalisationCHV1LocalisedMText> localisationCHV1LocalisedMText;

                            public List<V20KonfigurationHaftungshinweis.Inhalt.LocalisationCHV1MultilingualMText.LocalisedText.LocalisationCHV1LocalisedMText> getLocalisationCHV1LocalisedMText() {
                                if (localisationCHV1LocalisedMText == null) {
                                    localisationCHV1LocalisedMText = new ArrayList<V20KonfigurationHaftungshinweis.Inhalt.LocalisationCHV1MultilingualMText.LocalisedText.LocalisationCHV1LocalisedMText>();
                                }
                                return this.localisationCHV1LocalisedMText;
                            }


                            @XmlAccessorType(XmlAccessType.FIELD)
                            @XmlType(name = "", propOrder = {
                                    "language",
                                    "text"
                            })
                            public static class LocalisationCHV1LocalisedMText {

                                @XmlElement(name = "Language", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                                protected String language;
                                @XmlElement(name = "Text", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                                protected String text;

                                public String getLanguage() {
                                    return language;
                                }

                                public void setLanguage(String value) {
                                    this.language = value;
                                }

                                public String getText() {
                                    return text;
                                }

                                public void setText(String value) {
                                    this.text = value;
                                }

                            }

                        }

                    }

                }


                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "", propOrder = {
                        "localisationCHV1MultilingualText"
                })
                public static class Titel {

                    @XmlElement(name = "LocalisationCH_V1.MultilingualText", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                    protected V20KonfigurationHaftungshinweis.Titel.LocalisationCHV1MultilingualText localisationCHV1MultilingualText;

                    public V20KonfigurationHaftungshinweis.Titel.LocalisationCHV1MultilingualText getLocalisationCHV1MultilingualText() {
                        return localisationCHV1MultilingualText;
                    }

                    public void setLocalisationCHV1MultilingualText(V20KonfigurationHaftungshinweis.Titel.LocalisationCHV1MultilingualText value) {
                        this.localisationCHV1MultilingualText = value;
                    }


                    @XmlAccessorType(XmlAccessType.FIELD)
                    @XmlType(name = "", propOrder = {
                            "localisedText"
                    })
                    public static class LocalisationCHV1MultilingualText {

                        @XmlElement(name = "LocalisedText", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                        protected V20KonfigurationHaftungshinweis.Titel.LocalisationCHV1MultilingualText.LocalisedText localisedText;

                        public V20KonfigurationHaftungshinweis.Titel.LocalisationCHV1MultilingualText.LocalisedText getLocalisedText() {
                            return localisedText;
                        }

                        public void setLocalisedText(V20KonfigurationHaftungshinweis.Titel.LocalisationCHV1MultilingualText.LocalisedText value) {
                            this.localisedText = value;
                        }


                        @XmlAccessorType(XmlAccessType.FIELD)
                        @XmlType(name = "", propOrder = {
                                "localisationCHV1LocalisedText"
                        })
                        public static class LocalisedText {

                            @XmlElement(name = "LocalisationCH_V1.LocalisedText", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                            protected List<V20KonfigurationHaftungshinweis.Titel.LocalisationCHV1MultilingualText.LocalisedText.LocalisationCHV1LocalisedText> localisationCHV1LocalisedText;

                            public List<V20KonfigurationHaftungshinweis.Titel.LocalisationCHV1MultilingualText.LocalisedText.LocalisationCHV1LocalisedText> getLocalisationCHV1LocalisedText() {
                                if (localisationCHV1LocalisedText == null) {
                                    localisationCHV1LocalisedText = new ArrayList<V20KonfigurationHaftungshinweis.Titel.LocalisationCHV1MultilingualText.LocalisedText.LocalisationCHV1LocalisedText>();
                                }
                                return this.localisationCHV1LocalisedText;
                            }


                            @XmlAccessorType(XmlAccessType.FIELD)
                            @XmlType(name = "", propOrder = {
                                    "language",
                                    "text"
                            })
                            public static class LocalisationCHV1LocalisedText {

                                @XmlElement(name = "Language", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                                protected String language;
                                @XmlElement(name = "Text", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                                protected String text;

                                public String getLanguage() {
                                    return language;
                                }

                                public void setLanguage(String value) {
                                    this.language = value;
                                }

                                public String getText() {
                                    return text;
                                }

                                public void setText(String value) {
                                    this.text = value;
                                }

                            }

                        }

                    }

                }

            }


            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {
                    "titel",
                    "inhalt",
                    "auszugIndex"
            })
            public static class V20KonfigurationInformation {

                @XmlElement(name = "Titel", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                protected V20KonfigurationInformation.Titel titel;
                @XmlElement(name = "Inhalt", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                protected V20KonfigurationInformation.Inhalt inhalt;
                @XmlElement(name = "AuszugIndex", namespace = "http://www.interlis.ch/INTERLIS2.3")
                @XmlSchemaType(name = "unsignedByte")
                protected short auszugIndex;
                @XmlAttribute(name = "TID", required = true)
                @XmlSchemaType(name = "unsignedShort")
                protected int tid;

                public V20KonfigurationInformation.Titel getTitel() {
                    return titel;
                }

                public void setTitel(V20KonfigurationInformation.Titel value) {
                    this.titel = value;
                }

                public V20KonfigurationInformation.Inhalt getInhalt() {
                    return inhalt;
                }

                public void setInhalt(V20KonfigurationInformation.Inhalt value) {
                    this.inhalt = value;
                }

                public short getAuszugIndex() {
                    return auszugIndex;
                }

                public void setAuszugIndex(short value) {
                    this.auszugIndex = value;
                }

                public int getTID() {
                    return tid;
                }

                public void setTID(int value) {
                    this.tid = value;
                }


                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "", propOrder = {
                        "localisationCHV1MultilingualMText"
                })
                public static class Inhalt {

                    @XmlElement(name = "LocalisationCH_V1.MultilingualMText", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                    protected V20KonfigurationInformation.Inhalt.LocalisationCHV1MultilingualMText localisationCHV1MultilingualMText;

                    public V20KonfigurationInformation.Inhalt.LocalisationCHV1MultilingualMText getLocalisationCHV1MultilingualMText() {
                        return localisationCHV1MultilingualMText;
                    }

                    public void setLocalisationCHV1MultilingualMText(V20KonfigurationInformation.Inhalt.LocalisationCHV1MultilingualMText value) {
                        this.localisationCHV1MultilingualMText = value;
                    }


                    @XmlAccessorType(XmlAccessType.FIELD)
                    @XmlType(name = "", propOrder = {
                            "localisedText"
                    })
                    public static class LocalisationCHV1MultilingualMText {

                        @XmlElement(name = "LocalisedText", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                        protected V20KonfigurationInformation.Inhalt.LocalisationCHV1MultilingualMText.LocalisedText localisedText;

                        public V20KonfigurationInformation.Inhalt.LocalisationCHV1MultilingualMText.LocalisedText getLocalisedText() {
                            return localisedText;
                        }

                        public void setLocalisedText(V20KonfigurationInformation.Inhalt.LocalisationCHV1MultilingualMText.LocalisedText value) {
                            this.localisedText = value;
                        }


                        @XmlAccessorType(XmlAccessType.FIELD)
                        @XmlType(name = "", propOrder = {
                                "localisationCHV1LocalisedMText"
                        })
                        public static class LocalisedText {

                            @XmlElement(name = "LocalisationCH_V1.LocalisedMText", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                            protected List<V20KonfigurationInformation.Inhalt.LocalisationCHV1MultilingualMText.LocalisedText.LocalisationCHV1LocalisedMText> localisationCHV1LocalisedMText;

                            public List<V20KonfigurationInformation.Inhalt.LocalisationCHV1MultilingualMText.LocalisedText.LocalisationCHV1LocalisedMText> getLocalisationCHV1LocalisedMText() {
                                if (localisationCHV1LocalisedMText == null) {
                                    localisationCHV1LocalisedMText = new ArrayList<V20KonfigurationInformation.Inhalt.LocalisationCHV1MultilingualMText.LocalisedText.LocalisationCHV1LocalisedMText>();
                                }
                                return this.localisationCHV1LocalisedMText;
                            }


                            @XmlAccessorType(XmlAccessType.FIELD)
                            @XmlType(name = "", propOrder = {
                                    "language",
                                    "text"
                            })
                            public static class LocalisationCHV1LocalisedMText {

                                @XmlElement(name = "Language", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                                protected String language;
                                @XmlElement(name = "Text", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                                protected String text;

                                public String getLanguage() {
                                    return language;
                                }

                                public void setLanguage(String value) {
                                    this.language = value;
                                }

                                public String getText() {
                                    return text;
                                }

                                public void setText(String value) {
                                    this.text = value;
                                }

                            }

                        }

                    }

                }


                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "", propOrder = {
                        "localisationCHV1MultilingualText"
                })
                public static class Titel {

                    @XmlElement(name = "LocalisationCH_V1.MultilingualText", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                    protected V20KonfigurationInformation.Titel.LocalisationCHV1MultilingualText localisationCHV1MultilingualText;

                    public V20KonfigurationInformation.Titel.LocalisationCHV1MultilingualText getLocalisationCHV1MultilingualText() {
                        return localisationCHV1MultilingualText;
                    }

                    public void setLocalisationCHV1MultilingualText(V20KonfigurationInformation.Titel.LocalisationCHV1MultilingualText value) {
                        this.localisationCHV1MultilingualText = value;
                    }


                    @XmlAccessorType(XmlAccessType.FIELD)
                    @XmlType(name = "", propOrder = {
                            "localisedText"
                    })
                    public static class LocalisationCHV1MultilingualText {

                        @XmlElement(name = "LocalisedText", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                        protected V20KonfigurationInformation.Titel.LocalisationCHV1MultilingualText.LocalisedText localisedText;

                        public V20KonfigurationInformation.Titel.LocalisationCHV1MultilingualText.LocalisedText getLocalisedText() {
                            return localisedText;
                        }

                        public void setLocalisedText(V20KonfigurationInformation.Titel.LocalisationCHV1MultilingualText.LocalisedText value) {
                            this.localisedText = value;
                        }


                        @XmlAccessorType(XmlAccessType.FIELD)
                        @XmlType(name = "", propOrder = {
                                "localisationCHV1LocalisedText"
                        })
                        public static class LocalisedText {

                            @XmlElement(name = "LocalisationCH_V1.LocalisedText", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                            protected List<V20KonfigurationInformation.Titel.LocalisationCHV1MultilingualText.LocalisedText.LocalisationCHV1LocalisedText> localisationCHV1LocalisedText;

                            public List<V20KonfigurationInformation.Titel.LocalisationCHV1MultilingualText.LocalisedText.LocalisationCHV1LocalisedText> getLocalisationCHV1LocalisedText() {
                                if (localisationCHV1LocalisedText == null) {
                                    localisationCHV1LocalisedText = new ArrayList<V20KonfigurationInformation.Titel.LocalisationCHV1MultilingualText.LocalisedText.LocalisationCHV1LocalisedText>();
                                }
                                return this.localisationCHV1LocalisedText;
                            }


                            @XmlAccessorType(XmlAccessType.FIELD)
                            @XmlType(name = "", propOrder = {
                                    "language",
                                    "text"
                            })
                            public static class LocalisationCHV1LocalisedText {

                                @XmlElement(name = "Language", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                                protected String language;
                                @XmlElement(name = "Text", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                                protected String text;

                                public String getLanguage() {
                                    return language;
                                }

                                public void setLanguage(String value) {
                                    this.language = value;
                                }

                                public String getText() {
                                    return text;
                                }

                                public void setText(String value) {
                                    this.text = value;
                                }

                            }

                        }

                    }

                }

            }


            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {
                    "code",
                    "titel"
            })
            public static class V20KonfigurationRechtsStatusTxt {

                @XmlElement(name = "Code", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                protected String code;
                @XmlElement(name = "Titel", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                protected V20KonfigurationRechtsStatusTxt.Titel titel;
                @XmlAttribute(name = "TID", required = true)
                @XmlSchemaType(name = "unsignedByte")
                protected short tid;

                public String getCode() {
                    return code;
                }

                public void setCode(String value) {
                    this.code = value;
                }

                public V20KonfigurationRechtsStatusTxt.Titel getTitel() {
                    return titel;
                }

                public void setTitel(V20KonfigurationRechtsStatusTxt.Titel value) {
                    this.titel = value;
                }

                public short getTID() {
                    return tid;
                }

                public void setTID(short value) {
                    this.tid = value;
                }


                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "", propOrder = {
                        "localisationCHV1MultilingualText"
                })
                public static class Titel {

                    @XmlElement(name = "LocalisationCH_V1.MultilingualText", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                    protected V20KonfigurationRechtsStatusTxt.Titel.LocalisationCHV1MultilingualText localisationCHV1MultilingualText;

                    public V20KonfigurationRechtsStatusTxt.Titel.LocalisationCHV1MultilingualText getLocalisationCHV1MultilingualText() {
                        return localisationCHV1MultilingualText;
                    }

                    public void setLocalisationCHV1MultilingualText(V20KonfigurationRechtsStatusTxt.Titel.LocalisationCHV1MultilingualText value) {
                        this.localisationCHV1MultilingualText = value;
                    }


                    @XmlAccessorType(XmlAccessType.FIELD)
                    @XmlType(name = "", propOrder = {
                            "localisedText"
                    })
                    public static class LocalisationCHV1MultilingualText {

                        @XmlElement(name = "LocalisedText", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                        protected V20KonfigurationRechtsStatusTxt.Titel.LocalisationCHV1MultilingualText.LocalisedText localisedText;

                        public V20KonfigurationRechtsStatusTxt.Titel.LocalisationCHV1MultilingualText.LocalisedText getLocalisedText() {
                            return localisedText;
                        }

                        public void setLocalisedText(V20KonfigurationRechtsStatusTxt.Titel.LocalisationCHV1MultilingualText.LocalisedText value) {
                            this.localisedText = value;
                        }


                        @XmlAccessorType(XmlAccessType.FIELD)
                        @XmlType(name = "", propOrder = {
                                "localisationCHV1LocalisedText"
                        })
                        public static class LocalisedText {

                            @XmlElement(name = "LocalisationCH_V1.LocalisedText", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                            protected List<V20KonfigurationRechtsStatusTxt.Titel.LocalisationCHV1MultilingualText.LocalisedText.LocalisationCHV1LocalisedText> localisationCHV1LocalisedText;

                            public List<V20KonfigurationRechtsStatusTxt.Titel.LocalisationCHV1MultilingualText.LocalisedText.LocalisationCHV1LocalisedText> getLocalisationCHV1LocalisedText() {
                                if (localisationCHV1LocalisedText == null) {
                                    localisationCHV1LocalisedText = new ArrayList<V20KonfigurationRechtsStatusTxt.Titel.LocalisationCHV1MultilingualText.LocalisedText.LocalisationCHV1LocalisedText>();
                                }
                                return this.localisationCHV1LocalisedText;
                            }


                            @XmlAccessorType(XmlAccessType.FIELD)
                            @XmlType(name = "", propOrder = {
                                    "language",
                                    "text"
                            })
                            public static class LocalisationCHV1LocalisedText {

                                @XmlElement(name = "Language", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                                protected String language;
                                @XmlElement(name = "Text", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                                protected String text;

                                public String getLanguage() {
                                    return language;
                                }

                                public void setLanguage(String value) {
                                    this.language = value;
                                }

                                public String getText() {
                                    return text;
                                }

                                public void setText(String value) {
                                    this.text = value;
                                }

                            }

                        }

                    }

                }

            }

        }

    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
            "models",
            "comment"
    })
    public static class HEADERSECTION {

        @XmlElement(name = "MODELS", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
        protected V20Texte.HEADERSECTION.MODELS models;
        @XmlElement(name = "COMMENT", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
        protected String comment;
        @XmlAttribute(name = "SENDER", required = true)
        protected String sender;
        @XmlAttribute(name = "VERSION", required = true)
        protected BigDecimal version;

        public V20Texte.HEADERSECTION.MODELS getMODELS() {
            return models;
        }

        public void setMODELS(V20Texte.HEADERSECTION.MODELS value) {
            this.models = value;
        }

        public String getCOMMENT() {
            return comment;
        }

        public void setCOMMENT(String value) {
            this.comment = value;
        }

        public String getSENDER() {
            return sender;
        }

        public void setSENDER(String value) {
            this.sender = value;
        }

        public BigDecimal getVERSION() {
            return version;
        }

        public void setVERSION(BigDecimal value) {
            this.version = value;
        }


        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
                "model"
        })
        public static class MODELS {

            @XmlElement(name = "MODEL", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
            protected List<V20Texte.HEADERSECTION.MODELS.MODEL> model;

            public List<V20Texte.HEADERSECTION.MODELS.MODEL> getMODEL() {
                if (model == null) {
                    model = new ArrayList<V20Texte.HEADERSECTION.MODELS.MODEL>();
                }
                return this.model;
            }

            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "")
            public static class MODEL {

                @XmlAttribute(name = "NAME", required = true)
                protected String name;
                @XmlAttribute(name = "VERSION", required = true)
                @XmlSchemaType(name = "date")
                protected XMLGregorianCalendar version;
                @XmlAttribute(name = "URI", required = true)
                protected String uri;

                public String getNAME() {
                    return name;
                }

                public void setNAME(String value) {
                    this.name = value;
                }

                public XMLGregorianCalendar getVERSION() {
                    return version;
                }

                public void setVERSION(XMLGregorianCalendar value) {
                    this.version = value;
                }

                public String getURI() {
                    return uri;
                }

                public void setURI(String value) {
                    this.uri = value;
                }

            }
        }
    }
}

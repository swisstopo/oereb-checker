
package ch.swisstopo.oereb;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "headersection",
    "datasection"
})
@XmlRootElement(name = "TRANSFER", namespace = "http://www.interlis.ch/INTERLIS2.3")
public class V20Themen {

    @XmlElement(name = "HEADERSECTION", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
    protected V20Themen.HEADERSECTION headersection;
    @XmlElement(name = "DATASECTION", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
    protected V20Themen.DATASECTION datasection;

    public V20Themen.HEADERSECTION getHEADERSECTION() {
        return headersection;
    }

    public void setHEADERSECTION(V20Themen.HEADERSECTION value) {
        this.headersection = value;
    }

    public V20Themen.DATASECTION getDATASECTION() {
        return datasection;
    }

    public void setDATASECTION(V20Themen.DATASECTION value) {
        this.datasection = value;
    }


    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "v20Thema"
    })
    public static class DATASECTION {

        @XmlElement(name = "OeREBKRMkvs_V2_0.Thema", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
        protected V20Thema v20Thema;

        public V20Thema getV20Thema() {
            return v20Thema;
        }

        public void setV20Thema(V20Thema value) {
            this.v20Thema = value;
        }


        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "V20ThemaThemaOrV20ThemaThemaGesetz"
        })
        public static class V20Thema {

            @XmlElements({
                @XmlElement(name = "OeREBKRMkvs_V2_0.Thema.Thema", namespace = "http://www.interlis.ch/INTERLIS2.3", type = V20ThemaThema.class),
                @XmlElement(name = "OeREBKRMkvs_V2_0.Thema.ThemaGesetz", namespace = "http://www.interlis.ch/INTERLIS2.3", type = V20ThemaThemaGesetz.class)
            })
            protected List<Object> V20ThemaThemaOrV20ThemaThemaGesetz;
            @XmlAttribute(name = "BID", required = true)
            protected String bid;

            public List<Object> getV20ThemaThemaOrV20ThemaThemaGesetz() {
                if (V20ThemaThemaOrV20ThemaThemaGesetz == null) {
                    V20ThemaThemaOrV20ThemaThemaGesetz = new ArrayList<Object>();
                }
                return this.V20ThemaThemaOrV20ThemaThemaGesetz;
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
                "titel",
                "auszugIndex"
            })
            public static class V20ThemaThema {

                @XmlElement(name = "Code", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                protected String code;
                @XmlElement(name = "Titel", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                protected V20ThemaThema.Titel titel;
                @XmlElement(name = "AuszugIndex", namespace = "http://www.interlis.ch/INTERLIS2.3")
                @XmlSchemaType(name = "unsignedShort")
                protected int auszugIndex;
                @XmlAttribute(name = "TID", required = true)
                protected String tid;

                public String getCode() {
                    return code;
                }

                public void setCode(String value) {
                    this.code = value;
                }

                public V20ThemaThema.Titel getTitel() {
                    return titel;
                }

                public void setTitel(V20ThemaThema.Titel value) {
                    this.titel = value;
                }

                public int getAuszugIndex() {
                    return auszugIndex;
                }

                public void setAuszugIndex(int value) {
                    this.auszugIndex = value;
                }

                public String getTID() {
                    return tid;
                }

                public void setTID(String value) {
                    this.tid = value;
                }


                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "", propOrder = {
                    "localisationCHV1MultilingualText"
                })
                public static class Titel {

                    @XmlElement(name = "LocalisationCH_V1.MultilingualText", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                    protected V20ThemaThema.Titel.LocalisationCHV1MultilingualText localisationCHV1MultilingualText;

                    public V20ThemaThema.Titel.LocalisationCHV1MultilingualText getLocalisationCHV1MultilingualText() {
                        return localisationCHV1MultilingualText;
                    }

                    public void setLocalisationCHV1MultilingualText(V20ThemaThema.Titel.LocalisationCHV1MultilingualText value) {
                        this.localisationCHV1MultilingualText = value;
                    }


                    @XmlAccessorType(XmlAccessType.FIELD)
                    @XmlType(name = "", propOrder = {
                        "localisedText"
                    })
                    public static class LocalisationCHV1MultilingualText {

                        @XmlElement(name = "LocalisedText", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                        protected V20ThemaThema.Titel.LocalisationCHV1MultilingualText.LocalisedText localisedText;

                        public V20ThemaThema.Titel.LocalisationCHV1MultilingualText.LocalisedText getLocalisedText() {
                            return localisedText;
                        }

                        public void setLocalisedText(V20ThemaThema.Titel.LocalisationCHV1MultilingualText.LocalisedText value) {
                            this.localisedText = value;
                        }


                        @XmlAccessorType(XmlAccessType.FIELD)
                        @XmlType(name = "", propOrder = {
                            "localisationCHV1LocalisedText"
                        })
                        public static class LocalisedText {

                            @XmlElement(name = "LocalisationCH_V1.LocalisedText", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                            protected List<V20ThemaThema.Titel.LocalisationCHV1MultilingualText.LocalisedText.LocalisationCHV1LocalisedText> localisationCHV1LocalisedText;

                            public List<V20ThemaThema.Titel.LocalisationCHV1MultilingualText.LocalisedText.LocalisationCHV1LocalisedText> getLocalisationCHV1LocalisedText() {
                                if (localisationCHV1LocalisedText == null) {
                                    localisationCHV1LocalisedText = new ArrayList<V20ThemaThema.Titel.LocalisationCHV1MultilingualText.LocalisedText.LocalisationCHV1LocalisedText>();
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
                "thema",
                "gesetz"
            })
            public static class V20ThemaThemaGesetz {

                @XmlElement(name = "Thema", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                protected V20ThemaThemaGesetz.Thema thema;
                @XmlElement(name = "Gesetz", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
                protected V20ThemaThemaGesetz.Gesetz gesetz;

                public V20ThemaThemaGesetz.Thema getThema() {
                    return thema;
                }

                public void setThema(V20ThemaThemaGesetz.Thema value) {
                    this.thema = value;
                }

                public V20ThemaThemaGesetz.Gesetz getGesetz() {
                    return gesetz;
                }

                public void setGesetz(V20ThemaThemaGesetz.Gesetz value) {
                    this.gesetz = value;
                }


                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "")
                public static class Gesetz {

                    @XmlAttribute(name = "REF", required = true)
                    protected String ref;

                    public String getREF() {
                        return ref;
                    }

                    public void setREF(String value) {
                        this.ref = value;
                    }

                }


                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "")
                public static class Thema {

                    @XmlAttribute(name = "REF", required = true)
                    protected String ref;

                    public String getREF() {
                        return ref;
                    }

                    public void setREF(String value) {
                        this.ref = value;
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
        protected V20Themen.HEADERSECTION.MODELS models;
        @XmlElement(name = "COMMENT", namespace = "http://www.interlis.ch/INTERLIS2.3", required = true)
        protected String comment;
        @XmlAttribute(name = "SENDER", required = true)
        protected String sender;
        @XmlAttribute(name = "VERSION", required = true)
        protected BigDecimal version;

        public V20Themen.HEADERSECTION.MODELS getMODELS() {
            return models;
        }

        public void setMODELS(V20Themen.HEADERSECTION.MODELS value) {
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
            protected List<V20Themen.HEADERSECTION.MODELS.MODEL> model;

            public List<V20Themen.HEADERSECTION.MODELS.MODEL> getMODEL() {
                if (model == null) {
                    model = new ArrayList<V20Themen.HEADERSECTION.MODELS.MODEL>();
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

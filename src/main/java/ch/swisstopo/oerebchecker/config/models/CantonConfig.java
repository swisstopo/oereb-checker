package ch.swisstopo.oerebchecker.config.models;

import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "Config")
public class CantonConfig {

    public String ProxyHostname = "";
    public Integer ProxyPort = null;
    public String ProxyUser = "";
    public String ProxyPassword = "";

    public String Canton = "";
    public String BasicUrl = "";
    public Integer Threads = null;
    public String OutputDirectoryPath = "";

    public List<GetVersionsConfig> GetVersions = new ArrayList<>();
    public List<GetEGRIDConfig> GetEGRID = new ArrayList<>();
    public List<GetCapabilitiesConfig> GetCapabilities = new ArrayList<>();
    public List<GetExtractByIdConfig> GetExtractById = new ArrayList<>();
}
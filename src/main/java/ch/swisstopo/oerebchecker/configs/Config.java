package ch.swisstopo.oerebchecker.configs;

import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "Config")
public class Config {

    public String BasicUrl = "";
    public Integer Threads = null;
    public String OutputDirectoryPath = "";

    public List<GetVersionsConfig> GetVersions = new ArrayList<>();
    public List<GetEGRIDConfig> GetEGRID = new ArrayList<>();
    public List<GetCapabilitiesConfig> GetCapabilities = new ArrayList<>();
    public List<GetExtractByIdConfig> GetExtractById = new ArrayList<>();
}
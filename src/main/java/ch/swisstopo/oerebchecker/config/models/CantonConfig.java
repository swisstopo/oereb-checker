package ch.swisstopo.oerebchecker.config.models;

import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

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

    @Override
    public String toString() {
        StringJoiner j = new StringJoiner(", ", getClass().getSimpleName() + "[", "]");

        j.add("Canton=" + nullToEmpty(Canton));
        j.add("BasicUrl=" + nullToEmpty(BasicUrl));
        if (Threads != null) {
            j.add("Threads=" + Threads);
        }
        if (OutputDirectoryPath != null && !OutputDirectoryPath.isBlank()) {
            j.add("OutputDirectoryPath=" + OutputDirectoryPath);
        }

        // Proxy info without secrets
        if (ProxyHostname != null && !ProxyHostname.isBlank()) {
            j.add("ProxyHostname=" + ProxyHostname);
        }
        if (ProxyPort != null) {
            j.add("ProxyPort=" + ProxyPort);
        }
        if (ProxyUser != null && !ProxyUser.isBlank()) {
            j.add("ProxyUser=" + ProxyUser);
        }
        if (ProxyPassword != null && !ProxyPassword.isBlank()) {
            j.add("ProxyPassword=<masked>");
        }

        // Optional: show which check blocks exist (counts only)
        if (GetVersions != null) {
            j.add("GetVersions=" + GetVersions.size());
        }
        if (GetEGRID != null) {
            j.add("GetEGRID=" + GetEGRID.size());
        }
        if (GetCapabilities != null) {
            j.add("GetCapabilities=" + GetCapabilities.size());
        }
        if (GetExtractById != null) {
            j.add("GetExtractById=" + GetExtractById.size());
        }

        return j.toString();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
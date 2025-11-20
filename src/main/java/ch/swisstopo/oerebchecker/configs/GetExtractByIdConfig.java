package ch.swisstopo.oerebchecker.configs;

import java.util.ArrayList;
import java.util.List;

public class GetExtractByIdConfig extends CheckConfig {

    public boolean GEOMETRY = false;
    public boolean SIGNED = false;
    public String LANG = "de";
    public String TOPICS = "";
    public boolean WITHIMAGES = false;

    public String EGRID = null;
    public List<String> EGRIDS = new ArrayList<>();
    public String IDENTDN = null;
    public Integer NUMBER = null;
}

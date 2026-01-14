package ch.swisstopo.oerebchecker.config.models;

import ch.swisstopo.oerebchecker.models.ResponseFormat;
import software.amazon.awssdk.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class GetExtractByIdConfig extends CheckConfig {

    public Boolean GEOMETRY = null;
    public boolean SIGNED = false;
    public String LANG = "";
    public String TOPICS = "";
    public Boolean WITHIMAGES = null;

    public String EGRID = null;
    public List<String> EGRIDS = new ArrayList<>();
    public String IDENTDN = null;
    public String NUMBER = null;

    @Override
    public boolean isValid() {
        int variants = 0;

        if (!EGRIDS.isEmpty()) {
            variants++;
        }
        if (StringUtils.isNotBlank(EGRID)) {
            variants++;
        }
        if (StringUtils.isNotBlank(IDENTDN) && StringUtils.isNotBlank(NUMBER)) {
            variants++;
        }
        return variants == 1;
    }

    public List<GetExtractByIdConfig> getPossibleConfigs() {
        List<GetExtractByIdConfig> possibleConfigs = new ArrayList<>();
        possibleConfigs.add(getCopy());

        ResponseFormat responseFormat = ResponseFormat.valueOf(FORMAT);
        if (responseFormat == ResponseFormat.xml) {
            GetExtractByIdConfig copy;
            if (GEOMETRY == null) {
                copy = getCopy();
                copy.GEOMETRY = true;
                possibleConfigs.add(copy);
                copy = getCopy();
                copy.GEOMETRY = false;
                possibleConfigs.add(copy);
            }
            if (WITHIMAGES == null) {
                copy = getCopy();
                copy.WITHIMAGES = true;
                possibleConfigs.add(copy);
                copy = getCopy();
                copy.WITHIMAGES = false;
                possibleConfigs.add(copy);
            }
        }
        return possibleConfigs;
    }

    public GetExtractByIdConfig getCopy() {
        GetExtractByIdConfig copy = new GetExtractByIdConfig();
        copy.FORMAT = FORMAT;
        copy.GEOMETRY = GEOMETRY;
        copy.SIGNED = SIGNED;
        copy.LANG = LANG;
        copy.TOPICS = TOPICS;
        copy.WITHIMAGES = WITHIMAGES;

        copy.EGRID = EGRID;
        copy.EGRIDS = EGRIDS;
        copy.IDENTDN = IDENTDN;
        copy.NUMBER = NUMBER;

        copy.Provoke500 = Provoke500;
        copy.ExpectedStatusCode = ExpectedStatusCode;

        return copy;
    }
}

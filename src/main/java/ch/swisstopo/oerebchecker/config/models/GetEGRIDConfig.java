package ch.swisstopo.oerebchecker.config.models;

import software.amazon.awssdk.utils.StringUtils;

public class GetEGRIDConfig extends CheckConfig {

    public boolean GEOMETRY = false;

    public String EN = null;
    public String IDENTDN = null;
    public String NUMBER = null;
    public Integer POSTALCODE = null;
    public String LOCALISATION = null;
    public String GNSS = null;

    @Override
    public boolean isValid() {
        int variants = 0;

        if (StringUtils.isNotBlank(EN)) {
            variants++;
        }
        if (StringUtils.isNotBlank(IDENTDN) && StringUtils.isNotBlank(NUMBER)) {
            variants++;
        }
        if (POSTALCODE != null && StringUtils.isNotBlank(LOCALISATION) && StringUtils.isNotBlank(NUMBER)) {
            variants++;
        }
        if (StringUtils.isNotBlank(GNSS)) {
            variants++;
        }
        return variants == 1;
    }
}

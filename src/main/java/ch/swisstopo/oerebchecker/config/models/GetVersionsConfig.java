package ch.swisstopo.oerebchecker.config.models;

import software.amazon.awssdk.utils.StringUtils;

public class GetVersionsConfig extends CheckConfig {

    @Override
    public boolean isValid() {
        return StringUtils.isNotBlank(FORMAT);
    }
}

package ch.swisstopo.oerebchecker.config.models;

public abstract class CheckConfig {

    public String FORMAT;

    public Integer ExpectedStatusCode = null;

    public abstract boolean isValid();
}

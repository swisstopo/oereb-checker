package ch.swisstopo.oerebchecker.core.validation;

import software.amazon.awssdk.utils.StringUtils;

public class ValidatorMessage {

    public String Flavour = null;
    public String Rule = null;
    public String Message;
    public String Error;


    public ValidatorMessage(String message, String error) {
        Message = message;
        Error = error;
    }

    public ValidatorMessage(String flavour, String rule, String message, String error) {
        this(message, error);
        Flavour = flavour;
        Rule = rule;
    }

    @Override
    public String toString() {
        String result = "";

        if (StringUtils.isNotBlank(Rule)) {
            result += Rule;
        }
        if (StringUtils.isNotBlank(Flavour)) {
            if (StringUtils.isNotBlank(result)) {
                result += " - ";
            }
            result += Flavour;
        }
        if (StringUtils.isNotBlank(Message)) {
            if (StringUtils.isNotBlank(result)) {
                result += " - ";
            }
            result += Message;
        }
        if (StringUtils.isNotBlank(Error)) {
            if (StringUtils.isNotBlank(result)) {
                result += " - ";
            }
            result += Error;
        }

        return result.trim();
    }
}

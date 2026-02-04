package ch.swisstopo.oerebchecker.core.validation;

import software.amazon.awssdk.utils.StringUtils;

public class ValidatorMessage {

    public MessageSeverity Severity = MessageSeverity.ERROR;

    public String Flavour = null;
    public String Rule = null;
    public String Message;
    public String Error;

    protected ValidatorMessage(MessageSeverity severity, String message, String error) {
        Severity = severity;
        Message = message;
        Error = error;
    }

    protected ValidatorMessage(MessageSeverity severity, String flavour, String rule, String message, String error) {
        this(severity, message, error);
        Flavour = flavour;
        Rule = rule;
    }

    public static ValidatorMessage error(String message, String error) {
        return new ValidatorMessage(MessageSeverity.ERROR, message, error);
    }

    public static ValidatorMessage error(String flavour, String rule, String message, String error) {
        return new ValidatorMessage(MessageSeverity.ERROR, flavour, rule, message, error);
    }

    public static ValidatorMessage warning(String message, String error) {
        return new ValidatorMessage(MessageSeverity.WARNING, message, error);
    }

    public static ValidatorMessage warning(String flavour, String rule, String message, String error) {
        return new ValidatorMessage(MessageSeverity.WARNING, flavour, rule, message, error);
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

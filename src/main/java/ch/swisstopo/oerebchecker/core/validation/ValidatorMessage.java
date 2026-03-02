package ch.swisstopo.oerebchecker.core.validation;

import software.amazon.awssdk.utils.StringUtils;

public class ValidatorMessage {

    public MessageSeverity Severity;

    public String Flavour = "";
    public String Rule = "";
    public String Message;

    public String Location = "";
    public String Error;

    private static final String DEFAULT_FLAVOUR = "General";
    private static final String DEFAULT_RULE = "UNSPECIFIED";

    protected ValidatorMessage(MessageSeverity severity, String message, String location, String error) {
        Severity = severity;
        Message = message;
        Location = location;
        Error = error;
    }

    protected ValidatorMessage(MessageSeverity severity, String flavour, String rule, String message, String location, String error) {
        this(severity, message, location, error);
        Flavour = normaliseFlavour(flavour);
        Rule = normaliseRule(rule);
    }

    private static String normaliseRule(String rule) {
        return StringUtils.isNotBlank(rule) ? rule : DEFAULT_RULE;
    }

    private static String normaliseLocation(String location) {
        return location == null ? "" : location;
    }

    private static String normaliseFlavour(String flavour) {
        return StringUtils.isNotBlank(flavour) ? flavour : DEFAULT_FLAVOUR;
    }

    public static ValidatorMessage of(MessageSeverity severity, String flavour, String rule, String message, String location, String error) {
        return new ValidatorMessage(severity, flavour, rule, message, normaliseLocation(location), error);
    }

    public static ValidatorMessage error(String flavour, String rule, String message, String location, String error) {
        return of(MessageSeverity.ERROR, flavour, rule, message, location, error);
    }

    public static ValidatorMessage warning(String flavour, String rule, String message, String location, String error) {
        return of(MessageSeverity.WARNING, flavour, rule, message, location, error);
    }

    public static ValidatorMessage error(String message, String error) {
        return of(MessageSeverity.ERROR, DEFAULT_FLAVOUR, DEFAULT_RULE, message, null, error);
    }

    public static ValidatorMessage warning(String message, String error) {
        return of(MessageSeverity.WARNING, DEFAULT_FLAVOUR, DEFAULT_RULE, message, null, error);
    }

    // Backward-compatible structured overload (no explicit location)
    public static ValidatorMessage error(String flavour, String rule, String message, String error) {
        return of(MessageSeverity.ERROR, flavour, rule, message, null, error);
    }

    public static ValidatorMessage warning(String flavour, String rule, String message, String error) {
        return of(MessageSeverity.WARNING, flavour, rule, message, null, error);
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
        if (StringUtils.isNotBlank(Location)) {
            if (StringUtils.isNotBlank(result)) {
                result += " - ";
            }
            result += Location;
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

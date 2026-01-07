package ch.swisstopo.oerebchecker.core.validation;

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

        if (Rule != null) result += Rule + " - ";
        if (Flavour != null) result += Flavour + " - ";

        return (result + Message + " - " + Error).trim();
    }
}

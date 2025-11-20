package ch.swisstopo.oerebchecker.utils;

import java.util.ArrayList;
import java.util.List;

public class ValidatorResult {

    public Boolean IsValid = null;
    public List<ValidatorMessage> Messages;


    public ValidatorResult() {
        Messages = new ArrayList<>();
    }

    public void addMessage(String flavor, String rule, String message, String detail) {
        Messages.add(new ValidatorMessage(flavor, rule, message, detail));
    }

    public void addMessage(String message, String detail) {
        addMessage(null, null, message, detail);
    }

    public void addMessage(String message) {
        addMessage(message, null);
    }
}

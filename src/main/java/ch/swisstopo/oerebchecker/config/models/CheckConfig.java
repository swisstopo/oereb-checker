package ch.swisstopo.oerebchecker.config.models;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Objects;
import java.util.StringJoiner;

public abstract class CheckConfig {

    public String FORMAT;

    public boolean Provoke500 = false;
    public Integer ExpectedStatusCode = null;

    public abstract boolean isValid();

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", getClass().getSimpleName() + "[", "]");
        try {
            // Create a default instance to compare values against
            Object defaultInstance = getClass().getDeclaredConstructor().newInstance();

            for (Field field : getClass().getFields()) {
                Object value = field.get(this);
                Object defaultValue = field.get(defaultInstance);

                // Only log if value is not null, differs from default, and is not "empty"
                if (value != null && !Objects.deepEquals(value, defaultValue) && !isEmpty(value)) {
                    joiner.add(field.getName() + "=" + value);
                }
            }
        } catch (Exception e) {
            return getClass().getSimpleName() + "[Error generating log]";
        }
        return joiner.toString();
    }

    private boolean isEmpty(Object value) {
        if (value instanceof String) return ((String) value).isBlank();
        if (value instanceof Collection) return ((Collection<?>) value).isEmpty();
        return false;
    }
}

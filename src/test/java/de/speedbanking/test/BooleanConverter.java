package de.speedbanking.test;

import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ArgumentConverter;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Converter to transform various string representations into a boolean value.
 * <p>
 * supports "x", "ja", "true", and "1" as positive matches.
 */
public class BooleanConverter implements ArgumentConverter {

    private static final Set<String> POSITIVE_VALUES
        = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList("x", "ja", "true", "1")));

    /**
     * Converts the source object to a boolean.
     * <p>
     * If the source is already a Boolean, it is returned directly.
     * strings are trimmed and compared case-insensitively against whitelist.
     *
     * @param source  the object to convert
     * @param context the parameter context
     * @return the resulting boolean value
     * @throws ArgumentConversionException if source is not a String or Boolean
     */
    @Override
    public Object convert(Object source, ParameterContext context) throws ArgumentConversionException {
        if (source == null) {
            return false;
        } else if (source instanceof Boolean) {
            return source;
        } else if (!(source instanceof String)) {
            throw new ArgumentConversionException("Source must be a String or Boolean");
        }

        String input = ((String) source).toLowerCase().trim();
        return POSITIVE_VALUES.contains(input);
    }

}

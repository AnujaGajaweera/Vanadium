package net.vim.util;

import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class StructuredLog {
    private StructuredLog() {
    }

    public static void info(Logger logger, String event, Map<String, ?> details) {
        logger.info(format(event, details));
    }

    public static void warn(Logger logger, String event, Map<String, ?> details) {
        logger.warn(format(event, details));
    }

    public static void error(Logger logger, String event, Map<String, ?> details, Throwable error) {
        logger.error(format(event, details), error);
    }

    public static Map<String, Object> kv(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }

    private static String format(String event, Map<String, ?> details) {
        String payload = details.entrySet().stream()
                .map(entry -> entry.getKey() + '=' + String.valueOf(entry.getValue()))
                .collect(Collectors.joining(","));
        return "event=" + event + " " + payload;
    }
}

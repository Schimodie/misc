package org.schimodie.albums_to_listen_to.database;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.stream.Collectors;

public class StorageFileName {
    public static final DateTimeFormatter DT_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd")
            .parseDefaulting(ChronoField.NANO_OF_DAY, 0)
            .toFormatter()
            .withZone(ZoneOffset.UTC);

    private static final String FILE_PART_SEPARATOR = "-";

    private StorageFileName() {
    }

    public static String createFileName(Object... fileParts) {
        return String.format("%s.json",
                Arrays.stream(fileParts)
                        .map(StorageFileName::cast)
                        .collect(Collectors.joining(FILE_PART_SEPARATOR)));
    }

    private static String cast(Object object) {
        if (object == null) {
            return "null";
        } else if (object instanceof String) {
            return (String) object;
        } else if (object instanceof Instant) {
            return DT_FORMATTER.format((Instant) object);
        }

        return object.toString();
    }
}

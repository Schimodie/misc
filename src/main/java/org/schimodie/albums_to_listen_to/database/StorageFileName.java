package org.schimodie.albums_to_listen_to.database;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

public class StorageFileName {
    private static final String FILE_PART_SEPARATOR = "-";
    private static final DateTimeFormatter DT_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd")
            .parseDefaulting(ChronoField.NANO_OF_DAY, 0)
            .toFormatter()
            .withZone(ZoneOffset.UTC);

    private StorageFileName() {
    }

    public static String createFileName(String filePrefix, Instant date) {
        return createFileName(filePrefix, DT_FORMATTER.format(date));
    }

    public static String createFileName(String... fileParts) {
        return String.format("%s.json", String.join(FILE_PART_SEPARATOR, fileParts));
    }
}

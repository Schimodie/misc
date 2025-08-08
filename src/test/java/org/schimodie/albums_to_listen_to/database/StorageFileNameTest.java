package org.schimodie.albums_to_listen_to.database;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.schimodie.albums_to_listen_to.database.StorageFileName.DT_FORMATTER;

class StorageFileNameTest {
    @Test
    void testCreateFileName() {
        Instant afterDate = Instant.from(DT_FORMATTER.parse("2024-02-12"));
        Instant today = Instant.from(DT_FORMATTER.parse("2025-03-30"));
        String filteredAlbumsFileName = StorageFileName.createFileName(
                "filtered-albums-on", today, "from", afterDate);
        assertEquals("filtered-albums-on-2025-03-30-from-2024-02-12.json", filteredAlbumsFileName);
    }
}
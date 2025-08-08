package org.schimodie.albums_to_listen_to;

import lombok.Getter;
import org.schimodie.albums_to_listen_to.bean.Album;
import org.schimodie.albums_to_listen_to.client.MetalstormClient;
import org.schimodie.albums_to_listen_to.database.Storage;
import org.schimodie.albums_to_listen_to.database.StorageFileName;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AlbumsDownloader {
    @Getter
    private final String fileName;
    private final Instant afterDate;
    private final MetalstormClient msClient;

    public AlbumsDownloader(String fileNamePrefix, Instant afterDate) {
        Objects.requireNonNull(fileNamePrefix);
        Objects.requireNonNull(afterDate);

        this.afterDate = afterDate;
        this.fileName = StorageFileName.createFileName(fileNamePrefix, afterDate);
        this.msClient = new MetalstormClient();
    }

    public void downloadAlbums() {
        int page = 1;
        Instant dateOfLastAlbumOnThePage;
        List<Album> albums = new ArrayList<>();

        do {
            albums.addAll(msClient.getAlbums(page++));
            dateOfLastAlbumOnThePage = msClient.getAlbumDate(albums.get(albums.size() - 1).getAlbumId());
            albums.get(albums.size() - 1).setDate(dateOfLastAlbumOnThePage);
        } while (dateOfLastAlbumOnThePage.isAfter(afterDate) || dateOfLastAlbumOnThePage.equals(afterDate));

        Storage.writeAlbums(albums, fileName);
    }
}

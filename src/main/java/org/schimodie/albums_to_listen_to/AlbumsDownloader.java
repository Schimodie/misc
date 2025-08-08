package org.schimodie.albums_to_listen_to;

import lombok.Getter;
import org.schimodie.albums_to_listen_to.bean.Album;
import org.schimodie.albums_to_listen_to.client.MetalstormClient;
import org.schimodie.albums_to_listen_to.database.Storage;
import org.schimodie.albums_to_listen_to.database.StorageFileName;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AlbumsDownloader {
    @Getter
    private final String fileName;
    private final Instant afterDate;

    public AlbumsDownloader(String fileNamePrefix, Instant afterDate) {
        Objects.requireNonNull(fileNamePrefix);
        Objects.requireNonNull(afterDate);

        this.afterDate = afterDate;
        this.fileName = StorageFileName.createFileName(fileNamePrefix, afterDate);
    }

    public void downloadAlbums() {
        try (MetalstormClient msClient = new MetalstormClient()) {
            int page = 1;
            Instant dateOfLastAlbumOnThePage;
            List<Album> albums = new ArrayList<>();

            do {
                albums.addAll(msClient.getAlbums(page++));
                dateOfLastAlbumOnThePage = msClient.getAlbumDate(albums.getLast().getAlbumId());
                albums.getLast().setDate(dateOfLastAlbumOnThePage);
            } while (dateOfLastAlbumOnThePage.isAfter(afterDate) || dateOfLastAlbumOnThePage.equals(afterDate));

            Storage.writeAlbums(albums, fileName);
        }
    }
}

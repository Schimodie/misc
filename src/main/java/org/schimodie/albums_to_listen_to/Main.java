package org.schimodie.albums_to_listen_to;

import org.schimodie.albums_to_listen_to.bean.Album;
import org.schimodie.albums_to_listen_to.database.Storage;
import org.schimodie.albums_to_listen_to.database.StorageFileName;
import org.schimodie.common.Tuple2;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Comparator;
import java.util.List;

public class Main {
    private static final DateTimeFormatter DT_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd")
            .parseDefaulting(ChronoField.NANO_OF_DAY, 0)
            .toFormatter()
            .withZone(ZoneOffset.UTC);

    private static void printAlbums(List<Album> albums, String headerMessage) {
        System.out.println(headerMessage);
        albums.forEach(System.out::println);
    }

    public static void main(String[] args) {
        Instant afterDate = Instant.from(DT_FORMATTER.parse("2023-02-11"));
        String filteredAlbumsFileName = StorageFileName.createFileName("filtered-albums", afterDate);

        if (Storage.containsFile(filteredAlbumsFileName)) {
            List<Album> albums = Storage.readAlbums(filteredAlbumsFileName);
            printAlbums(albums, "Fetched already filtered albums: " + albums.size());
            return;
        }

        AlbumsDownloader albumsDownloader = new AlbumsDownloader("albums", afterDate);

        if (!Storage.containsFile(albumsDownloader.getFileName())) {
            albumsDownloader.downloadAlbums();
        }

        AlbumsFilter albumsFilter = new AlbumsFilter(Storage.readAlbums(albumsDownloader.getFileName()));
        Tuple2<List<Album>, List<Album>> albums = albumsFilter.filter();
        AlbumList goodAlbums = new AlbumList(albums.t1())
                .sortBy(Comparator.comparingInt(Album::getVotes))
                .sortByPriority();

        List<Album> highPriorityGoodAlbums = goodAlbums.getHighPriorityAlbums();
        printAlbums(highPriorityGoodAlbums, "Number of high priority good albums: " + highPriorityGoodAlbums.size());

        List<Album> lowPriorityGoodAlbums = goodAlbums.getLowPriorityAlbums();
        printAlbums(lowPriorityGoodAlbums, "\nNumber of low priority good albums: " + lowPriorityGoodAlbums.size());

        printAlbums(albums.t2(), "\nNumber of bad albums: " + albums.t2().size());

        Storage.writeAlbums(albums.t1(), filteredAlbumsFileName);
    }
}

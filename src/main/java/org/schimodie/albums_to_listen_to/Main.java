package org.schimodie.albums_to_listen_to;

import org.schimodie.albums_to_listen_to.bean.Album;
import org.schimodie.albums_to_listen_to.database.Storage;
import org.schimodie.albums_to_listen_to.database.StorageFileName;
import org.schimodie.common.data.Tuple2;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import static org.schimodie.albums_to_listen_to.database.StorageFileName.DT_FORMATTER;

public class Main {
    private static void printAlbums(List<Album> albums, String headerMessage) {
        System.out.println(headerMessage);
        albums.forEach(System.out::println);
    }

    public static void main(String[] args) {
        Instant afterDate = Instant.from(DT_FORMATTER.parse("2025-06-29"));
        Instant today = Instant.from(DT_FORMATTER.parse("2025-08-08"));
        String filteredAlbumsFileName = StorageFileName.createFileName(
                "filtered-albums-on", today, "from", afterDate);
        List<Album> albums;

        if (Storage.containsFile(filteredAlbumsFileName)) {
            albums = Storage.readAlbums(filteredAlbumsFileName);
            printAlbums(albums, "Fetched already filtered albums: " + albums.size());
        } else {
            AlbumsDownloader albumsDownloader = new AlbumsDownloader("albums", afterDate);

            if (!Storage.containsFile(albumsDownloader.getFileName())) {
                albumsDownloader.downloadAlbums();
            }

            albums = Storage.readAlbums(albumsDownloader.getFileName());
        }

        AlbumsFilter albumsFilter = new AlbumsFilter(albums);
        Tuple2<List<Album>, List<Album>> filteredAlbums = albumsFilter.filter();
        AlbumList goodAlbums = new AlbumList(filteredAlbums.t1())
                .sortBy(Comparator.comparingInt(Album::getVotes))
                .sortByPriority();

        List<Album> highPriorityGoodAlbums = goodAlbums.getHighPriorityAlbums();
        printAlbums(highPriorityGoodAlbums, "Number of high priority good albums: " + highPriorityGoodAlbums.size());

        List<Album> lowPriorityGoodAlbums = goodAlbums.getLowPriorityAlbums();
        printAlbums(lowPriorityGoodAlbums, "\nNumber of low priority good albums: " + lowPriorityGoodAlbums.size());

        printAlbums(filteredAlbums.t2(), "\nNumber of bad albums: " + filteredAlbums.t2().size());

        Storage.writeAlbums(filteredAlbums.t1(), filteredAlbumsFileName);
    }
}

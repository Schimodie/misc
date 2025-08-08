package org.schimodie.albums_to_listen_to;

import com.fasterxml.jackson.databind.JsonNode;
import org.schimodie.albums_to_listen_to.bean.Album;
import org.schimodie.albums_to_listen_to.client.LastFMClient;
import org.schimodie.common.data.Tuple2;

import java.util.ArrayList;
import java.util.List;

public class AlbumsFilter {
    private static final double epsilon = 0.0000001d;

    private final LastFMClient lastFMClient;
    private final List<Album> problemAlbums;
    private final List<Album> albums;

    private int processedAlbums;

    public AlbumsFilter(List<Album> albums) {
        this.lastFMClient = new LastFMClient();
        this.problemAlbums = new ArrayList<>();
        this.albums = albums;
    }

    public Tuple2<List<Album>, List<Album>> filter() {
        problemAlbums.clear();
        List<Album> filteredList = albums.stream()
                .filter(album -> !album.getGenre().contains("grind")
                        && (album.getType().equalsIgnoreCase("studio") || album.getType().equalsIgnoreCase("ep")))
                .filter(AlbumsFilter::filterByRatingAndVotes)
                .filter(this::filterByNoListensOnLastFM)
                .toList();
        return Tuple2.of(filteredList, problemAlbums);
    }

    private boolean filterByNoListensOnLastFM(Album album) {
        JsonNode albumInfo = lastFMClient.getAlbumInfo(album.getArtists().get(0), album.getAlbum());
        JsonNode albumNode = albumInfo == null || albumInfo.isNull() || albumInfo.isMissingNode()
                ? null
                : albumInfo.get("album");
        JsonNode userPlayCount = albumNode == null || albumNode.isNull() || albumNode.isMissingNode()
                ? null
                : albumNode.get("userplaycount");

        ++processedAlbums;
        if (processedAlbums % 10 == 0) {
            System.out.println("Processed albums: " + processedAlbums);
        }

        if (userPlayCount == null || userPlayCount.isMissingNode() || userPlayCount.isNull()) {
            problemAlbums.add(album);
            return false;
        }

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return userPlayCount.asInt(1) == 0;
    }

    private static boolean filterByRatingAndVotes(Album album) {
        return (album.getRating() >= 7.8 && album.getVotes() >= 10)
                || (Math.abs(album.getRating() - 7.7) <= epsilon && album.getVotes() >= 12)
                || (Math.abs(album.getRating() - 7.6) <= epsilon && album.getVotes() >= 13)
                || (Math.abs(album.getRating() - 7.5) <= epsilon && album.getVotes() >= 15)
                || (Math.abs(album.getRating() - 7.4) <= epsilon && album.getVotes() >= 17)
                || (7.2 <= album.getRating() && album.getRating() <= 7.3 && album.getVotes() >= 20)
                || (Math.abs(album.getRating() - 7.1) <= epsilon && album.getVotes() >= 23)
                || (Math.abs(album.getRating() - 7.0) <= epsilon && album.getVotes() >= 25);
    }
}

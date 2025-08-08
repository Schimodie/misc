package org.schimodie.albums_to_listen_to;

import org.schimodie.albums_to_listen_to.bean.Album;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record AlbumList(List<Album> albums) {
    private static final Comparator<Album> PRIORITY_COMPARATOR =
            (a1, a2) -> (int) Math.signum(a1.computePriorityRating() - a2.computePriorityRating());

    public AlbumList(List<Album> albums) {
        this.albums = new ArrayList<>(Objects.requireNonNull(albums, "'albums' should not be null"));
    }

    public AlbumList sortBy(Comparator<Album> comparator) {
        albums.sort(comparator);
        return this;
    }

    public AlbumList sortByPriority() {
        return sortBy(PRIORITY_COMPARATOR);
    }

    public List<Album> getHighPriorityAlbums() {
        return albums.stream().filter(album -> album.computePriorityRating() >= 8.0).toList();
    }

    public List<Album> getLowPriorityAlbums() {
        return albums.stream().filter(album -> album.computePriorityRating() < 8.0).toList();
    }
}

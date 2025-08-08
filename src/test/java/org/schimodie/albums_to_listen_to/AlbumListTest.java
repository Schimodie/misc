package org.schimodie.albums_to_listen_to;

import org.junit.jupiter.api.Test;
import org.schimodie.albums_to_listen_to.bean.Album;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AlbumListTest {
    @Test
    void sorting() {
        AlbumList albums = new AlbumList(Arrays.asList(
                album("a", 7.8, 28),
                album("b", 7.7, 19),
                album("c", 7.5, 200),
                album("d", 7.5, 100),
                album("e", 7.8, 29),
                album("f", 7.8, 28),
                album("g", 7.7, 41),
                album("h", 7.7, 32),
                album("i", 8.9, 200),
                album("j", 7.9, 309)
        ));

        List<Album> expected = Arrays.asList(
                album("d", 7.5, 100),
                album("c", 7.5, 200),
                album("b", 7.7, 19),
                album("a", 7.8, 28),
                album("f", 7.8, 28),
                album("e", 7.8, 29),
                album("h", 7.7, 32),
                album("g", 7.7, 41),
                album("j", 7.9, 309),
                album("i", 8.9, 200)
        );

        albums.sortBy(Comparator.comparingInt(Album::getVotes)).sortByPriority();

        assertEquals(expected, albums.albums());
    }

    private static Album album(String album, double rating, int votes) {
        return Album.builder()
                .album(album)
                .rating(rating)
                .votes(votes)
                .build();
    }
}
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
                album("a", 7.8, 28),   // score: 8.015
                album("b", 7.7, 19),   // score: 7.783
                album("c", 7.5, 200),  // score: 8.286
                album("d", 7.5, 100),  // score: 8.074
                album("e", 7.8, 29),   // score: 8.019
                album("f", 7.8, 28),   // score: 8.015
                album("g", 7.7, 41),   // score: 7.924
                album("h", 7.7, 32),   // score: 7.882
                album("i", 8.9, 200),  // score: 9.686
                album("j", 7.9, 309)   // score: 8.314
        ));

        List<Album> expected = Arrays.asList(
                album("b", 7.7, 19),   // 7.783
                album("h", 7.7, 32),   // 7.882
                album("g", 7.7, 41),   // 7.924
                album("a", 7.8, 28),   // 8.015
                album("f", 7.8, 28),   // 8.015
                album("e", 7.8, 29),   // 8.019
                album("d", 7.5, 100),  // 8.074
                album("c", 7.5, 200),  // 8.286
                album("j", 7.9, 309),  // 8.314
                album("i", 8.9, 200)   // 9.686
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
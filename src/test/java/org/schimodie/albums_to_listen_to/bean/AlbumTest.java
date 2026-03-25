package org.schimodie.albums_to_listen_to.bean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class AlbumTest {
    private static final double DOUBLE_DELTA = 1e-5;

    @ParameterizedTest
    @MethodSource("computeScoreSource")
    void computeScore(double rating, int votes, double expectedScore) {
        Album album = Album.builder().rating(rating).votes(votes).build();
        assertEquals(expectedScore, album.getScore(), DOUBLE_DELTA);
    }

    private static Stream<Arguments> computeScoreSource() {
        return Stream.of(
                // No boost: votes below threshold
                arguments(7.5, 0, 7.5),
                arguments(7.5, 9, 7.5),
                arguments(6.0, 5, 6.0),
                // Left sigmoid: fast rise from 10 to 70
                arguments(7.5, 10, 7.583),
                arguments(7.5, 50, 7.81),
                // Midpoint: votes = 70 → boost ≈ 0.5
                arguments(7.5, 70, 8.0),
                // Right sigmoid: slow taper from 70 to 500
                arguments(7.5, 100, 8.074),
                arguments(7.5, 150, 8.19),
                arguments(7.5, 300, 8.409),
                arguments(7.5, 500, 8.487),
                // Cap: votes above 500 treated as 500
                arguments(7.5, 600, 8.487),
                // All albums get boost regardless of rating
                arguments(5.0, 70, 5.5),
                arguments(9.5, 70, 10.0)
        );
    }
}
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
    @MethodSource("computePriorityRatingSource")
    void computePriorityRating(double rating, int votes, double expectedPriorityRating) {
        Album album = Album.builder()
                .rating(rating)
                .votes(votes)
                .build();
        assertEquals(expectedPriorityRating, album.computePriorityRating(), DOUBLE_DELTA);
    }

    private static Stream<Arguments> computePriorityRatingSource() {
        return Stream.of(
                arguments(7.5, 100, 7.5),
                arguments(7.6, 100, 7.6),
                arguments(7.7, 0, 7.7),
                arguments(7.7, 10, 7.7),
                arguments(7.7, 20, 7.7),
                arguments(7.7, 29, 7.7),
                arguments(7.7, 30, 7.8),
                arguments(7.8, 20, 7.8),
                arguments(7.7, 40, 7.9),
                arguments(7.8, 30, 7.9),
                arguments(7.9, 20, 7.9)
        );
    }
}
package org.schimodie.albums_to_listen_to.bean;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"score", "rating", "votes", "artists", "album", "artistIds", "albumId", "genres", "type", "date"})
public class Album {
    private static final ObjectMapper OM = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);

    private static final int SIGMOID_MIDPOINT = 70;
    private static final double LEFT_STEEPNESS = 0.04;
    private static final double RIGHT_STEEPNESS = 0.01;
    private static final int MIN_VOTES_FOR_BOOST = 10;
    private static final int MAX_VOTES_FOR_BOOST = 500;

    private double score;

    private double rating;

    @JsonAlias({"numVotes"})
    private int votes;

    private List<String> artists;

    private String album;

    private List<String> artistIds;

    private String albumId;

    @JsonAlias({"genre"})
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> genres;

    private String type;

    private Instant date;

    public double getScore() {
        if (score < 1.0) {
            score = computeScore();
        }
        return score;
    }

    @Override
    @SneakyThrows
    public String toString() {
        return OM.writeValueAsString(this);
    }

    private double computeScore() {
        if (votes < MIN_VOTES_FOR_BOOST) {
            return Math.round(rating * 1000.0) / 1000.0;
        }

        int clamped = Math.min(votes, MAX_VOTES_FOR_BOOST);
        double k = clamped < SIGMOID_MIDPOINT ? LEFT_STEEPNESS : RIGHT_STEEPNESS;
        double boost = 1.0 / (1.0 + Math.exp(-k * (clamped - SIGMOID_MIDPOINT)));

        return Math.round((rating + boost) * 1000.0) / 1000.0;
    }

    @SneakyThrows
    public static Album from(String string) {
        return OM.readValue(string.trim(), Album.class);
    }
}

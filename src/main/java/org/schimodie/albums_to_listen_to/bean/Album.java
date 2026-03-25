package org.schimodie.albums_to_listen_to.bean;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
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
public class Album {
    private static final ObjectMapper OM = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);

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

    private static double round(double value) {
        return Math.floor(value * 10) / 10;
    }

    @SneakyThrows
    public static Album from(String string) {
        return OM.readValue(string.trim(), Album.class);
    }

    public double computePriorityRating() {
        return rating + (rating < 7.7 ? 0.0 : round(Math.max(votes - 20, 0) / 100.0));
    }

    @Override
    @SneakyThrows
    public String toString() {
        return OM.writeValueAsString(this);
    }
}

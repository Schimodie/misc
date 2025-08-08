package org.schimodie.albums_to_listen_to.bean;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public record Pair(String first, String second) implements Comparable<Pair> {
    @Override
    public int compareTo(Pair that) {
        return this.first.compareTo(that.first);
    }

    public static Pair encodedOf(String first, String second) {
        return new Pair(URLEncoder.encode(first, StandardCharsets.UTF_8),
                URLEncoder.encode(second, StandardCharsets.UTF_8));
    }

    public static Pair of(String first, String second) {
        return new Pair(first, second);
    }
}

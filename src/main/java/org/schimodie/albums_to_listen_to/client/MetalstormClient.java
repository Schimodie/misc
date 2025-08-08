package org.schimodie.albums_to_listen_to.client;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.schimodie.albums_to_listen_to.bean.Album;
import org.schimodie.common.utils.ExponentialBackoffRetryable;
import org.schimodie.common.utils.Retryable;

import java.io.IOException;
import java.net.CookieStore;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MetalstormClient {
    private static final Retryable<Document> RETRY_STRATEGY = new ExponentialBackoffRetryable<>(5, 1000);
    private static final Map<String, String> DEFAULT_HEADERS = Map.ofEntries(
            Map.entry("Connection", "keep-alive"),
            Map.entry("DNT", "1"),
            Map.entry("User-Agent",
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:102.0) Gecko/20100101 Firefox/102.0"),
            Map.entry("Accept",
                    "ext/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"),
            Map.entry("Accept-Encoding", "gzip, deflate, br"),
            Map.entry("Accept-Language", "en-US,en;q=0.5"),
            Map.entry("TE", "trailers"),
            Map.entry("Upgrade-Insecure-Requests", "1"));
    private static final String ALBUM_DATE_CSS_QUERY =
            ".right-col > table:nth-child(2) > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(2)";
    private static final String ROOT_URL = "https://metalstorm.net/";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("d MMMM yyyy")
            .parseDefaulting(ChronoField.NANO_OF_DAY, 0)
            .toFormatter()
            .withZone(ZoneOffset.UTC);

    private CookieStore cookieStore;
    private boolean hasFilterBeenSet = false;

    public List<Album> getAlbums(int page) {
        if (!hasFilterBeenSet) {
            getDocument(ROOT_URL + "bands/albums.php?filter_show_invisible=1&show_invisible_submit=1");
            hasFilterBeenSet = true;
        }

        List<Album> albums = getDocument(ROOT_URL + "bands/albums.php?page=" + page)
                .select(".table > tbody:nth-child(2) > tr")
                .stream()
                .map(MetalstormClient::createAlbum)
                .toList();

        System.out.printf("Fetched %d albums from page %d of Metalstorm%n", albums.size(), page);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return albums;
    }

    public Instant getAlbumDate(String albumId) {
        return Optional.ofNullable(getDocument(ROOT_URL + "bands/album.php?album_id=" + albumId)
                        .selectFirst(ALBUM_DATE_CSS_QUERY))
                .map(Element::text)
                .map(DATE_TIME_FORMATTER::parse)
                .map(Instant::from)
                .orElseThrow(() -> new RuntimeException("Couldn't parse album date"));
    }

    private Document getDocument(String url) {
        Connection connection = Jsoup.connect(url).headers(DEFAULT_HEADERS);
        Document doc;

        if (cookieStore == null) {
            try {
                doc = connection.get();
                cookieStore = doc.connection().cookieStore();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                doc = connection.cookieStore(cookieStore).get();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return doc;
    }

    private Document postDocument(String url, String... payload) {
        Connection connection = Jsoup.connect(url).headers(DEFAULT_HEADERS);
        Document doc;

        if (cookieStore == null) {
            try {
                doc = connection.data(payload).followRedirects(true).post();
                cookieStore = doc.connection().cookieStore();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                doc = connection.cookieStore(cookieStore).data(payload).followRedirects(true).post();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return doc;
    }

    private static Album createAlbum(Element albumRow) {
        Elements albumColumns = albumRow.getElementsByTag("td");
        Elements artistAndAlbumNames = albumColumns.get(2).getElementsByTag("a");
        List<String> artists = artistAndAlbumNames.stream()
                .filter(el -> el.attr("href").contains("band_id"))
                .map(Element::text)
                .toList();
        List<String> artistId = artistAndAlbumNames.stream()
                .filter(el -> el.attr("href").contains("band_id"))
                .map(el -> el.attr("href").split("=")[1])
                .toList();
        String album = artistAndAlbumNames.stream()
                .filter(el -> el.attr("href").contains("album_id"))
                .map(Element::text)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Couldn't find the album"));
        String albumId = artistAndAlbumNames.stream()
                .filter(el -> el.attr("href").contains("album_id"))
                .map(el -> el.attr("href").split("=")[1])
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Couldn't find the albumId"));
        String type = albumColumns.get(4).text();
        String genre = albumColumns.get(5).text();
        double rating;
        int numVotes;

        try {
            Elements ratingElements = albumColumns.get(6).getElementsByTag("a");
            if (ratingElements.isEmpty()) {
                rating = 0.0;
            } else {
                rating = Double.parseDouble(ratingElements.get(0).text());
            }
        } catch (NumberFormatException e) {
            rating = 0.0;
        }

        try {
            numVotes = Integer.parseInt(albumColumns.get(7).text());
        } catch (NumberFormatException e) {
            numVotes = 0;
        }

        return Album.builder()
                .album(album)
                .artists(artists)
                .albumId(albumId)
                .artistIds(artistId)
                .genre(genre)
                .type(type)
                .rating(rating)
                .votes(numVotes)
                .build();
    }
}

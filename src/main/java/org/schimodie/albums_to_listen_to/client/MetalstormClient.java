package org.schimodie.albums_to_listen_to.client;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.schimodie.albums_to_listen_to.bean.Album;
import org.schimodie.common.utils.ExponentialBackoffRetryable;
import org.schimodie.common.utils.Retryable;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MetalstormClient implements AutoCloseable {
    static final String ALBUM_DATE_SELECTOR =
            ".right-col > table:nth-child(2) > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(2)";
    static final String ALBUM_ROWS_SELECTOR = ".discography-album .right-col .album-title-row";

    private static final Retryable<String> RETRY_STRATEGY = new ExponentialBackoffRetryable<>(5, 1000);
    private static final Map<String, String> DEFAULT_HEADERS = Map.ofEntries(
            Map.entry("Connection", "keep-alive"),
            Map.entry("DNT", "1"),
            Map.entry("Accept",
                    "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"),
            Map.entry("Accept-Encoding", "gzip, deflate, br"),
            Map.entry("Accept-Language", "en-US,en;q=0.5"),
            Map.entry("TE", "trailers"),
            Map.entry("Upgrade-Insecure-Requests", "1"));
    private static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:143.0) Gecko/20100101 Firefox/143.0";
    private static final String ROOT_URL = "https://metalstorm.net";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("d MMMM yyyy")
            .parseDefaulting(ChronoField.NANO_OF_DAY, 0)
            .toFormatter()
            .withZone(ZoneOffset.UTC);

    private final Playwright playwright;
    private final Browser browser;
    private final BrowserContext context;
    private boolean hasFilterBeenSet = false;

    public MetalstormClient() {
        this.playwright = Playwright.create();
        this.browser = playwright.firefox().launch(new BrowserType.LaunchOptions().setHeadless(true));
        this.context = browser.newContext(
                new Browser.NewContextOptions().setUserAgent(USER_AGENT).setExtraHTTPHeaders(DEFAULT_HEADERS));
    }

    private static Album createAlbum(ElementHandle albumTitleRow) {
        ElementHandle albumTitle = albumTitleRow.querySelector(".album-title");
        List<ElementHandle> artistAndAlbumLinks = albumTitle.querySelectorAll(".megatitle a");
        List<ElementHandle> darkSpans = albumTitle.querySelectorAll("span.dark");
        ElementHandle albumRating = albumTitleRow.querySelector(".album-rating");

        return Album.builder()
                .artists(artistAndAlbumLinks.stream().
                        filter(anchor -> {
                            String href = anchor.getAttribute("href");
                            return href != null && href.matches("/bands/band.php\\?band_id=[0-9]+");
                        })
                        .map(ElementHandle::innerText)
                        .toList())
                .artistIds(artistAndAlbumLinks.stream()
                        .filter(anchor -> {
                            String href = anchor.getAttribute("href");
                            return href != null && href.matches("/bands/band.php\\?band_id=[0-9]+");
                        })
                        .map(anchor -> {
                            String href = anchor.getAttribute("href");
                            return href.split("=")[1];
                        })
                        .toList())
                .album(artistAndAlbumLinks.stream()
                        .filter(anchor -> {
                            String href = anchor.getAttribute("href");
                            return href != null && href.matches("/bands/album.php\\?album_id=[0-9]+");
                        })
                        .map(ElementHandle::innerText)
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Couldn't find the album")))
                .albumId(artistAndAlbumLinks.stream()
                        .filter(anchor -> {
                            String href = anchor.getAttribute("href");
                            return href != null && href.matches("/bands/album.php\\?album_id=[0-9]+");
                        })
                        .map(anchor -> {
                            String href = anchor.getAttribute("href");
                            return href.split("=")[1];
                        })
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Couldn't find the albumId")))
                .date(Instant.from(DATE_TIME_FORMATTER.parse(darkSpans.getLast().innerText())))
                .type(darkSpans.size() > 1 ? darkSpans.getFirst().innerText().replaceAll("[\\[\\]]", "") : "Studio")
                .genres(Arrays.stream(albumTitleRow.querySelector("div:nth-child(2) > span").innerText().split(","))
                        .map(String::trim)
                        .toList())
                .rating(Optional.ofNullable(albumRating.querySelector(".megarating"))
                        .map(ElementHandle::innerText)
                        .map(Double::parseDouble)
                        .orElse(0.0))
                .votes(Optional.ofNullable(albumRating.querySelector(".votes_num"))
                        .map(ElementHandle::innerText)
                        .map(votes -> votes.replaceAll("votes?", ""))
                        .map(String::trim)
                        .map(Integer::parseInt)
                        .orElse(0))
                .build();
    }

    public List<Album> getAlbums(int pageNumber) {
        try (Page page = context.newPage()) {
            if (!hasFilterBeenSet) {
                String url = String.format("%s/events/new_releases.php", ROOT_URL);

                page.navigate(url);
                page.waitForLoadState();
                page.click(".g-2 > div:nth-child(2) > button:nth-child(1)");
                page.waitForLoadState();

                hasFilterBeenSet = true;
            }

            page.navigate(String.format("%s/events/new_releases.php?page=%d", ROOT_URL, pageNumber));
            page.waitForLoadState();

            List<ElementHandle> albumRows = page.querySelectorAll(ALBUM_ROWS_SELECTOR);
            List<Album> albums = albumRows.stream()
                    .map(MetalstormClient::createAlbum)
                    .toList();

            System.out.printf("Fetched %d albums from page %d of MetalStorm%n", albums.size(), pageNumber);
            Thread.sleep(100);

            return albums;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Instant getAlbumDate(String albumId) {
        try (Page page = context.newPage()) {
            String url = String.format("%s/bands/album.php?album_id=%s", ROOT_URL, albumId);

            page.navigate(url);
            page.waitForLoadState();

            ElementHandle dateElement = page.querySelector(ALBUM_DATE_SELECTOR);
            if (dateElement == null) {
                throw new RuntimeException(String.format("Couldn't parse date for album: %s", url));
            }
            return Instant.from(DATE_TIME_FORMATTER.parse(dateElement.innerText()));
        }
    }

    @Override
    public void close() {
        if (context != null) {
            context.close();
        }

        if (browser != null) {
            browser.close();
        }

        if (playwright != null) {
            playwright.close();
        }
    }
}

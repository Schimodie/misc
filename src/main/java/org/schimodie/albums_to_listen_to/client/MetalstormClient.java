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
import java.util.List;
import java.util.Map;

public class MetalstormClient implements AutoCloseable {
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
    private static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:139.0) Gecko/20100101 Firefox/139.0";
    private static final String ALBUM_DATE_CSS_QUERY =
            ".right-col > table:nth-child(2) > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(2)";
    private static final String ROOT_URL = "https://metalstorm.net/";
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

    private static Album createAlbum(ElementHandle albumRow) {
        List<ElementHandle> albumColumns = albumRow.querySelectorAll("td");

        if (albumColumns.size() < 8) {
            throw new RuntimeException("Invalid album row structure");
        }

        ElementHandle artistAlbumColumn = albumColumns.get(2);
        List<ElementHandle> artistAndAlbumLinks = artistAlbumColumn.querySelectorAll("a");

        List<String> artists = artistAndAlbumLinks.stream()
                .filter(el -> {
                    String href = el.getAttribute("href");
                    return href != null && href.contains("band_id");
                })
                .map(ElementHandle::innerText)
                .toList();

        List<String> artistIds = artistAndAlbumLinks.stream()
                .filter(el -> {
                    String href = el.getAttribute("href");
                    return href != null && href.contains("band_id");
                })
                .map(el -> {
                    String href = el.getAttribute("href");
                    return href.split("=")[1];
                })
                .toList();

        String album = artistAndAlbumLinks.stream()
                .filter(el -> {
                    String href = el.getAttribute("href");
                    return href != null && href.contains("album_id");
                })
                .map(ElementHandle::innerText)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Couldn't find the album"));

        String albumId = artistAndAlbumLinks.stream()
                .filter(el -> {
                    String href = el.getAttribute("href");
                    return href != null && href.contains("album_id");
                })
                .map(el -> {
                    String href = el.getAttribute("href");
                    return href.split("=")[1];
                })
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Couldn't find the albumId"));

        String type = albumColumns.get(4).innerText();
        String genre = albumColumns.get(5).innerText();

        double rating = 0.0;
        int numVotes = 0;

        try {
            List<ElementHandle> ratingElements = albumColumns.get(6).querySelectorAll("a");
            if (!ratingElements.isEmpty()) {
                String ratingText = ratingElements.getFirst().innerText();
                rating = Double.parseDouble(ratingText);
            }
        } catch (NumberFormatException _) {
        }

        try {
            String votesText = albumColumns.get(7).innerText();
            numVotes = Integer.parseInt(votesText);
        } catch (NumberFormatException _) {
        }

        return Album.builder()
                .album(album)
                .artists(artists)
                .albumId(albumId)
                .artistIds(artistIds)
                .genre(genre)
                .type(type)
                .rating(rating)
                .votes(numVotes)
                .build();
    }

    public List<Album> getAlbums(int pageNumber) {
        if (!hasFilterBeenSet) {
            navigateToUrl(ROOT_URL + "bands/albums.php?filter_show_invisible=1&show_invisible_submit=1");
            hasFilterBeenSet = true;
        }

        try (Page page = context.newPage()) {
            navigateToUrl(ROOT_URL + "bands/albums.php?page=" + pageNumber);
            page.waitForSelector(".table > tbody:nth-child(2) > tr");

            List<ElementHandle> albumRows = page.querySelectorAll(".table > tbody:nth-child(2) > tr");
            List<Album> albums = albumRows.stream()
                    .map(MetalstormClient::createAlbum)
                    .toList();

            System.out.printf("Fetched %d albums from page %d of Metalstorm%n", albums.size(), pageNumber);
            Thread.sleep(100);

            return albums;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Instant getAlbumDate(String albumId) {
        try (Page page = context.newPage()) {
            navigateToUrl(ROOT_URL + "bands/album.php?album_id=" + albumId);
            page.waitForSelector(ALBUM_DATE_CSS_QUERY);

            ElementHandle dateElement = page.querySelector(ALBUM_DATE_CSS_QUERY);
            if (dateElement != null) {
                String dateText = dateElement.innerText();
                return DATE_TIME_FORMATTER.parse(dateText, Instant::from);
            }
            throw new RuntimeException("Couldn't parse album date");
        }
    }

    private void navigateToUrl(String url) {
        try (Page page = context.newPage()) {
            page.navigate(url);
            page.waitForLoadState();
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

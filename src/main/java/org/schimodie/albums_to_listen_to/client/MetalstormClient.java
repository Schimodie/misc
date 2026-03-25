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
    static final String ALBUM_ROWS_SELECTOR = "#page-content .cbox.mb-4 table.table-striped > tbody > tr";
    static final String FILTER_BUTTON_SELECTOR = ".g-2 > div:nth-child(2) > button:nth-child(1)";

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

    private static Album createAlbum(ElementHandle albumRow) {
        ElementHandle contentCell = albumRow.querySelector("td.w-100");
        List<ElementHandle> artistLinks = contentCell.querySelectorAll("a[href*='band_id']");
        ElementHandle albumLink = contentCell.querySelector("a[href*='album_id']");
        ElementHandle ratingCol = contentCell.querySelector("[data-bs-title]");
        ElementHandle typeSpan = contentCell.querySelector(".col-lg-2 span.dark:not(.d-md-none)");
        ElementHandle genreCol = contentCell.querySelector(".col-lg-3");
        ElementHandle dateSpan = albumRow.querySelector("td.d-none.d-md-table-cell .megatitle > span.dark:last-of-type");

        return Album.builder()
                .artists(artistLinks.stream()
                        .map(ElementHandle::innerText)
                        .toList())
                .artistIds(artistLinks.stream()
                        .map(anchor -> anchor.getAttribute("href").split("=")[1])
                        .toList())
                .album(albumLink.innerText())
                .albumId(albumLink.getAttribute("href").split("=")[1])
                .date(Instant.from(DATE_TIME_FORMATTER.parse(
                        dateSpan.innerText().replaceAll("\\s+", " ").trim())))
                .type(typeSpan != null
                        ? typeSpan.innerText().replaceAll("[\\[\\]]", "").trim()
                        : "Studio")
                .genres(genreCol != null
                        ? Arrays.stream(genreCol.innerText().split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .toList()
                        : List.of())
                .rating(Optional.ofNullable(ratingCol)
                        .map(col -> col.querySelector("span.bold"))
                        .map(ElementHandle::innerText)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Double::parseDouble)
                        .orElse(0.0))
                .votes(Optional.ofNullable(ratingCol)
                        .map(col -> col.getAttribute("data-bs-title"))
                        .map(tooltip -> tooltip.replaceAll("[^0-9]", ""))
                        .filter(s -> !s.isEmpty())
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
                page.click(FILTER_BUTTON_SELECTOR);
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

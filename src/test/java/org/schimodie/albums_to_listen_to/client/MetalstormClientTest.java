package org.schimodie.albums_to_listen_to.client;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.schimodie.albums_to_listen_to.bean.Album;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.withinPercentage;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetalstormClientTest {
    private static MockedStatic<Playwright> mockedPlaywrightStatic;

    @Mock
    private Playwright mockPlaywright;
    @Mock
    private Browser mockBrowser;
    @Mock
    private BrowserContext mockContext;
    @Mock
    private Page mockPage;
    @Mock
    private BrowserType mockFirefox;

    private MetalstormClient metalstormClient;

    @BeforeAll
    static void setUpClass() {
        // Setup static mock for Playwright.create() for all tests
        mockedPlaywrightStatic = mockStatic(Playwright.class);
    }

    @AfterAll
    static void tearDownClass() {
        // Clean up static mock after all tests complete
        if (mockedPlaywrightStatic != null) {
            mockedPlaywrightStatic.close();
        }
    }

    @BeforeEach
    void setUp() {
        // Setup basic mock behavior for MetalstormClient constructor
        when(mockPlaywright.firefox()).thenReturn(mockFirefox);
        when(mockFirefox.launch(any(BrowserType.LaunchOptions.class))).thenReturn(mockBrowser);
        when(mockBrowser.newContext(any(Browser.NewContextOptions.class))).thenReturn(mockContext);

        // Configure static mock to return our Playwright mock
        mockedPlaywrightStatic.when(Playwright::create).thenReturn(mockPlaywright);

        // Create client - now uses the properly managed static mock
        metalstormClient = new MetalstormClient();

        // Setup basic mock behavior for test execution
        lenient().when(mockContext.newPage()).thenReturn(mockPage);
        lenient().when(mockPage.navigate(anyString())).thenReturn(null); // navigate returns Response
        lenient().doNothing().when(mockPage).waitForLoadState();
        lenient().doNothing().when(mockPage).close();
        lenient().doNothing().when(mockContext).close();
        lenient().doNothing().when(mockBrowser).close();
        lenient().doNothing().when(mockPlaywright).close();
    }

    private ElementHandle createMockAlbumRow() {
        ElementHandle mockRow = mock(ElementHandle.class);
        List<ElementHandle> columns = createMockColumns();
        when(mockRow.querySelectorAll("td")).thenReturn(columns);
        return mockRow;
    }

    private List<ElementHandle> createMockColumns() {
        // Create 8 columns as required by MetalstormClient
        return List.of(
                mock(ElementHandle.class), // column 0
                mock(ElementHandle.class), // column 1
                createMockArtistAlbumColumn(), // column 2 - artist/album
                mock(ElementHandle.class), // column 3
                createMockColumn("Full-length"), // column 4 - type
                createMockColumn("Metal"), // column 5 - genre
                createMockRatingColumn(), // column 6 - rating
                createMockColumn("50") // column 7 - votes
        );
    }

    private ElementHandle createMockArtistAlbumColumn() {
        ElementHandle column = mock(ElementHandle.class);

        ElementHandle artistLink = mock(ElementHandle.class);
        when(artistLink.getAttribute("href")).thenReturn("band.php?band_id=12345");
        when(artistLink.innerText()).thenReturn("Test Artist");

        ElementHandle albumLink = mock(ElementHandle.class);
        when(albumLink.getAttribute("href")).thenReturn("album.php?album_id=67890");
        when(albumLink.innerText()).thenReturn("Test Album");

        when(column.querySelectorAll("a")).thenReturn(List.of(artistLink, albumLink));
        return column;
    }

    private ElementHandle createMockRatingColumn() {
        ElementHandle column = mock(ElementHandle.class);
        ElementHandle ratingLink = mock(ElementHandle.class);
        when(ratingLink.innerText()).thenReturn("8.5");
        when(column.querySelectorAll("a")).thenReturn(List.of(ratingLink));
        return column;
    }

    private ElementHandle createMockColumn(String text) {
        ElementHandle column = mock(ElementHandle.class);
        when(column.innerText()).thenReturn(text);
        return column;
    }

    private ElementHandle createMockRowWithInsufficientColumns() {
        ElementHandle mockRow = mock(ElementHandle.class);
        List<ElementHandle> insufficientColumns = List.of(
                mock(ElementHandle.class),
                mock(ElementHandle.class),
                mock(ElementHandle.class)  // Only 3 columns, need at least 8
        );
        when(mockRow.querySelectorAll("td")).thenReturn(insufficientColumns);
        return mockRow;
    }

    private ElementHandle createMockRowWithMissingAlbum() {
        ElementHandle mockRow = mock(ElementHandle.class);
        List<ElementHandle> columns = List.of(
                mock(ElementHandle.class), mock(ElementHandle.class),
                createMockColumnWithOnlyArtist(), // missing album link
                mock(ElementHandle.class), mock(ElementHandle.class),
                mock(ElementHandle.class), mock(ElementHandle.class), mock(ElementHandle.class)
        );
        when(mockRow.querySelectorAll("td")).thenReturn(columns);
        return mockRow;
    }

    private ElementHandle createMockColumnWithOnlyArtist() {
        ElementHandle column = mock(ElementHandle.class);
        ElementHandle artistLink = mock(ElementHandle.class);
        when(artistLink.getAttribute("href")).thenReturn("band.php?band_id=12345");
        when(artistLink.innerText()).thenReturn("Test Artist");
        when(column.querySelectorAll("a")).thenReturn(List.of(artistLink));
        return column;
    }

    private ElementHandle createMockRowWithInvalidRating() {
        ElementHandle mockRow = mock(ElementHandle.class);
        List<ElementHandle> columns = List.of(
                mock(ElementHandle.class), mock(ElementHandle.class), createMockArtistAlbumColumn(),
                mock(ElementHandle.class), createMockColumn("Full-length"), createMockColumn("Metal"),
                createMockInvalidRatingColumn(), createMockColumn("50")
        );
        when(mockRow.querySelectorAll("td")).thenReturn(columns);
        return mockRow;
    }

    private ElementHandle createMockInvalidRatingColumn() {
        ElementHandle column = mock(ElementHandle.class);
        ElementHandle ratingLink = mock(ElementHandle.class);
        when(ratingLink.innerText()).thenReturn("invalid");
        when(column.querySelectorAll("a")).thenReturn(List.of(ratingLink));
        return column;
    }

    private ElementHandle createMockRowWithInvalidVotes() {
        ElementHandle mockRow = mock(ElementHandle.class);
        List<ElementHandle> columns = List.of(
                mock(ElementHandle.class), mock(ElementHandle.class), createMockArtistAlbumColumn(),
                mock(ElementHandle.class), createMockColumn("Full-length"), createMockColumn("Metal"),
                createMockRatingColumn(), createMockColumn("invalid")
        );
        when(mockRow.querySelectorAll("td")).thenReturn(columns);
        return mockRow;
    }

    private ElementHandle createMockAlbumRowWithMultipleArtists() {
        ElementHandle mockRow = mock(ElementHandle.class);
        List<ElementHandle> columns = List.of(
                mock(ElementHandle.class), // column 0
                mock(ElementHandle.class), // column 1
                createMockArtistAlbumColumnWithMultipleArtists(), // column 2 - multiple artists/album
                mock(ElementHandle.class), // column 3
                createMockColumn("Full-length"), // column 4 - type
                createMockColumn("Metal"), // column 5 - genre
                createMockRatingColumn(), // column 6 - rating
                createMockColumn("50") // column 7 - votes
        );
        when(mockRow.querySelectorAll("td")).thenReturn(columns);
        return mockRow;
    }

    private ElementHandle createMockArtistAlbumColumnWithMultipleArtists() {
        ElementHandle column = mock(ElementHandle.class);

        ElementHandle artist1Link = mock(ElementHandle.class);
        when(artist1Link.getAttribute("href")).thenReturn("band.php?band_id=11111");
        when(artist1Link.innerText()).thenReturn("Artist One");

        ElementHandle artist2Link = mock(ElementHandle.class);
        when(artist2Link.getAttribute("href")).thenReturn("band.php?band_id=22222");
        when(artist2Link.innerText()).thenReturn("Artist Two");

        ElementHandle albumLink = mock(ElementHandle.class);
        when(albumLink.getAttribute("href")).thenReturn("album.php?album_id=67890");
        when(albumLink.innerText()).thenReturn("Test Album");

        when(column.querySelectorAll("a")).thenReturn(List.of(artist1Link, artist2Link, albumLink));
        return column;
    }

    private ElementHandle createMockAlbumRowWithZeroValues() {
        ElementHandle mockRow = mock(ElementHandle.class);
        List<ElementHandle> columns = List.of(
                mock(ElementHandle.class), // column 0
                mock(ElementHandle.class), // column 1
                createMockArtistAlbumColumn(), // column 2 - artist/album
                mock(ElementHandle.class), // column 3
                createMockColumn("Full-length"), // column 4 - type
                createMockColumn("Metal"), // column 5 - genre
                createMockZeroRatingColumn(), // column 6 - rating
                createMockColumn("0") // column 7 - votes
        );
        when(mockRow.querySelectorAll("td")).thenReturn(columns);
        return mockRow;
    }

    private ElementHandle createMockZeroRatingColumn() {
        ElementHandle column = mock(ElementHandle.class);
        ElementHandle ratingLink = mock(ElementHandle.class);
        when(ratingLink.innerText()).thenReturn("0.0");
        when(column.querySelectorAll("a")).thenReturn(List.of(ratingLink));
        return column;
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {
        @Test
        @DisplayName("should initialize Playwright components correctly")
        void testConstructorInitializesPlaywright() {
            // Given & When - constructor already called in setUp
            // Then - verify the components are properly initialized (tested in setUp)
            assertThat(metalstormClient).isNotNull();
        }
    }

    @Nested
    @DisplayName("getAlbums Tests")
    class GetAlbumsTests {
        @Test
        @DisplayName("should return list of albums from valid page")
        void testGetAlbumsReturnsAlbumList() {
            // Given
            int pageNumber = 1;
            ElementHandle mockRow = createMockAlbumRow();
            when(mockPage.querySelectorAll(".table > tbody:nth-child(2) > tr")).thenReturn(List.of(mockRow));
            when(mockPage.waitForSelector(".table > tbody:nth-child(2) > tr")).thenReturn(mock(ElementHandle.class));

            // When
            List<Album> albums = metalstormClient.getAlbums(pageNumber);

            // Then
            assertThat(albums).hasSize(1);
            Album album = albums.getFirst();
            assertThat(album.getAlbum()).isEqualTo("Test Album");
            assertThat(album.getArtists()).isEqualTo(List.of("Test Artist"));
            assertThat(album.getGenre()).isEqualTo("Metal");
            assertThat(album.getRating()).isCloseTo(8.5, withinPercentage(0.1));
            assertThat(album.getVotes()).isEqualTo(50);

            verify(mockPage).navigate("https://metalstorm.net/bands/albums.php?page=" + pageNumber);
        }

        @Test
        @DisplayName("should handle empty page gracefully")
        void testGetAlbumsHandlesEmptyPage() {
            // Given
            int pageNumber = 999;
            when(mockPage.querySelectorAll(".table > tbody:nth-child(2) > tr")).thenReturn(List.of());
            when(mockPage.waitForSelector(".table > tbody:nth-child(2) > tr")).thenReturn(mock(ElementHandle.class));

            // When
            List<Album> albums = metalstormClient.getAlbums(pageNumber);

            // Then
            assertThat(albums).isEmpty();
        }

        @Test
        @DisplayName("should set filter on first call")
        void testGetAlbumsSetFilterOnFirstCall() {
            // Given
            when(mockPage.querySelectorAll(".table > tbody:nth-child(2) > tr")).thenReturn(List.of());
            when(mockPage.waitForSelector(".table > tbody:nth-child(2) > tr")).thenReturn(mock(ElementHandle.class));

            // When
            metalstormClient.getAlbums(1);

            // Then - verify filter URL was called first
            verify(mockPage).navigate(
                    "https://metalstorm.net/bands/albums.php?filter_show_invisible=1&show_invisible_submit=1");
            verify(mockPage).navigate("https://metalstorm.net/bands/albums.php?page=1");
        }

        @Test
        @DisplayName("should handle multiple albums on a page")
        void testGetAlbumsHandlesMultipleAlbums() {
            // Given
            int pageNumber = 1;
            ElementHandle mockRow1 = createMockAlbumRow();
            ElementHandle mockRow2 = createMockAlbumRow();
            when(mockPage.querySelectorAll(".table > tbody:nth-child(2) > tr")).thenReturn(List.of(mockRow1, mockRow2));
            when(mockPage.waitForSelector(".table > tbody:nth-child(2) > tr")).thenReturn(mock(ElementHandle.class));

            // When
            List<Album> albums = metalstormClient.getAlbums(pageNumber);

            // Then
            assertThat(albums).hasSize(2);
            albums.forEach(album -> {
                assertThat(album.getAlbum()).isEqualTo("Test Album");
                assertThat(album.getArtists()).isEqualTo(List.of("Test Artist"));
                assertThat(album.getGenre()).isEqualTo("Metal");
                assertThat(album.getRating()).isCloseTo(8.5, withinPercentage(0.1));
                assertThat(album.getVotes()).isEqualTo(50);
            });

            verify(mockPage).navigate("https://metalstorm.net/bands/albums.php?page=" + pageNumber);
        }

        @Test
        @DisplayName("should handle invalid row structure gracefully")
        void testGetAlbumsHandlesInvalidRowStructure() {
            // Given
            ElementHandle mockRow = createMockRowWithInsufficientColumns();
            when(mockPage.querySelectorAll(".table > tbody:nth-child(2) > tr")).thenReturn(List.of(mockRow));
            when(mockPage.waitForSelector(".table > tbody:nth-child(2) > tr")).thenReturn(mock(ElementHandle.class));

            // When & Then
            assertThatThrownBy(() -> metalstormClient.getAlbums(1))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Invalid album row structure");
        }

        @Test
        @DisplayName("should handle missing album link gracefully")
        void testGetAlbumsHandlesMissingAlbumLink() {
            // Given
            ElementHandle mockRow = createMockRowWithMissingAlbum();
            when(mockPage.querySelectorAll(".table > tbody:nth-child(2) > tr")).thenReturn(List.of(mockRow));
            when(mockPage.waitForSelector(".table > tbody:nth-child(2) > tr")).thenReturn(mock(ElementHandle.class));

            // When & Then
            assertThatThrownBy(() -> metalstormClient.getAlbums(1))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Couldn't find the album");
        }

        @Test
        @DisplayName("should handle invalid rating gracefully")
        void testGetAlbumsHandlesInvalidRating() {
            // Given
            ElementHandle mockRow = createMockRowWithInvalidRating();
            when(mockPage.querySelectorAll(".table > tbody:nth-child(2) > tr")).thenReturn(List.of(mockRow));
            when(mockPage.waitForSelector(".table > tbody:nth-child(2) > tr")).thenReturn(mock(ElementHandle.class));

            // When
            List<Album> albums = metalstormClient.getAlbums(1);

            // Then
            assertThat(albums).hasSize(1);
            assertThat(albums.getFirst().getRating()).isCloseTo(0.0, withinPercentage(0.1));
        }

        @Test
        @DisplayName("should handle invalid votes gracefully")
        void testGetAlbumsHandlesInvalidVotes() {
            // Given
            ElementHandle mockRow = createMockRowWithInvalidVotes();
            when(mockPage.querySelectorAll(".table > tbody:nth-child(2) > tr")).thenReturn(List.of(mockRow));
            when(mockPage.waitForSelector(".table > tbody:nth-child(2) > tr")).thenReturn(mock(ElementHandle.class));

            // When
            List<Album> albums = metalstormClient.getAlbums(1);

            // Then
            assertThat(albums).hasSize(1);
            assertThat(albums.getFirst().getVotes()).isEqualTo(0);
        }

        @Test
        @DisplayName("should extract all Album fields correctly")
        void testGetAlbumsExtractsAllFields() {
            // Given
            int pageNumber = 1;
            ElementHandle mockRow = createMockAlbumRow();
            when(mockPage.querySelectorAll(".table > tbody:nth-child(2) > tr")).thenReturn(List.of(mockRow));
            when(mockPage.waitForSelector(".table > tbody:nth-child(2) > tr")).thenReturn(mock(ElementHandle.class));

            // Expected Album using Album.builder() with all fields
            Album expectedAlbum = Album.builder()
                    .album("Test Album")
                    .artists(List.of("Test Artist"))
                    .artistIds(List.of("12345"))
                    .albumId("67890")
                    .genre("Metal")
                    .type("Full-length")
                    .rating(8.5)
                    .votes(50)
                    // Note: date field is not set by getAlbums, only by getAlbumDate
                    .build();

            // When
            List<Album> albums = metalstormClient.getAlbums(pageNumber);

            // Then - assert full Album equality
            assertThat(albums).hasSize(1);
            Album actualAlbum = albums.getFirst();

            assertThat(actualAlbum).isEqualTo(expectedAlbum);
            // Date is null for getAlbums - only getAlbumDate sets it
            assertThat(actualAlbum.getDate()).isNull();

            verify(mockPage).navigate("https://metalstorm.net/bands/albums.php?page=" + pageNumber);
        }

        @Test
        @DisplayName("should handle albums with multiple artists")
        void testGetAlbumsWithMultipleArtists() {
            // Given
            int pageNumber = 1;
            ElementHandle mockRow = createMockAlbumRowWithMultipleArtists();
            when(mockPage.querySelectorAll(".table > tbody:nth-child(2) > tr")).thenReturn(List.of(mockRow));
            when(mockPage.waitForSelector(".table > tbody:nth-child(2) > tr")).thenReturn(mock(ElementHandle.class));

            // When
            List<Album> albums = metalstormClient.getAlbums(pageNumber);

            // Then
            assertThat(albums).hasSize(1);
            Album album = albums.getFirst();
            assertThat(album.getArtists()).containsExactly("Artist One", "Artist Two");
            assertThat(album.getArtistIds()).containsExactly("11111", "22222");
            assertThat(album.getAlbumId()).isEqualTo("67890");
        }

        @Test
        @DisplayName("should handle albums with zero votes and rating")
        void testGetAlbumsWithZeroVotesAndRating() {
            // Given
            int pageNumber = 1;
            ElementHandle mockRow = createMockAlbumRowWithZeroValues();
            when(mockPage.querySelectorAll(".table > tbody:nth-child(2) > tr")).thenReturn(List.of(mockRow));
            when(mockPage.waitForSelector(".table > tbody:nth-child(2) > tr")).thenReturn(mock(ElementHandle.class));

            // When
            List<Album> albums = metalstormClient.getAlbums(pageNumber);

            // Then
            assertThat(albums).hasSize(1);
            Album album = albums.getFirst();
            assertThat(album.getRating()).isCloseTo(0.0, withinPercentage(0.1));
            assertThat(album.getVotes()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("getAlbumDate Tests")
    class GetAlbumDateTests {
        @Test
        @DisplayName("should parse album date correctly")
        void testGetAlbumDateParsesCorrectly() {
            // Given
            String albumId = "12345";
            ElementHandle mockDateElement = mock(ElementHandle.class);
            when(mockDateElement.innerText()).thenReturn("15 November 2024");
            when(mockPage.querySelector(
                    ".right-col > table:nth-child(2) > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(2)"))
                    .thenReturn(mockDateElement);
            when(mockPage.waitForSelector(anyString())).thenReturn(mock(ElementHandle.class));

            // When
            Instant result = metalstormClient.getAlbumDate(albumId);

            // Then
            Instant expected = Instant.parse("2024-11-15T00:00:00Z");
            assertThat(result).isEqualTo(expected);
            verify(mockPage).navigate("https://metalstorm.net/bands/album.php?album_id=" + albumId);
        }

        @Test
        @DisplayName("should throw exception when date element is null")
        void testGetAlbumDateThrowsOnNullElement() {
            // Given
            String albumId = "12345";
            when(mockPage.querySelector(anyString())).thenReturn(null);
            when(mockPage.waitForSelector(anyString())).thenReturn(mock(ElementHandle.class));

            // When & Then
            assertThatThrownBy(() -> metalstormClient.getAlbumDate(albumId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Couldn't parse album date");
        }

        @ParameterizedTest
        @ValueSource(strings = {"1 January 2024", "31 December 2023", "29 February 2024"})
        @DisplayName("should parse various date formats")
        void testGetAlbumDateParsesVariousFormats(String dateString) {
            // Given
            String albumId = "12345";
            ElementHandle mockDateElement = mock(ElementHandle.class);
            when(mockDateElement.innerText()).thenReturn(dateString);
            when(mockPage.querySelector(anyString())).thenReturn(mockDateElement);
            when(mockPage.waitForSelector(anyString())).thenReturn(mock(ElementHandle.class));

            // When
            Instant result = metalstormClient.getAlbumDate(albumId);

            // Then
            assertThat(result).isNotNull();
            verify(mockPage).navigate("https://metalstorm.net/bands/album.php?album_id=" + albumId);
        }

        @Test
        @DisplayName("should return correct Instant for album date")
        void testGetAlbumDateReturnsCorrectInstant() {
            // Given
            String albumId = "12345";
            ElementHandle mockDateElement = mock(ElementHandle.class);
            when(mockDateElement.innerText()).thenReturn("25 December 2023");
            when(mockPage.querySelector(anyString())).thenReturn(mockDateElement);
            when(mockPage.waitForSelector(anyString())).thenReturn(mock(ElementHandle.class));

            // When
            Instant result = metalstormClient.getAlbumDate(albumId);

            // Then
            Instant expected = Instant.parse("2023-12-25T00:00:00Z");
            assertThat(result)
                    .isNotNull()
                    .isEqualTo(expected);

            // Verify the correct navigation occurred
            verify(mockPage).navigate("https://metalstorm.net/bands/album.php?album_id=" + albumId);
        }
    }

    @Nested
    @DisplayName("Resource Management Tests")
    class ResourceManagementTests {
        @Test
        @DisplayName("should close all resources properly")
        void testCloseReleasesResources() {
            // When
            metalstormClient.close();

            // Then
            verify(mockContext).close();
            verify(mockBrowser).close();
            verify(mockPlaywright).close();
        }

        @Test
        @DisplayName("should handle multiple close calls gracefully")
        void testMultipleCloseCalls() {
            // When & Then - should not throw
            assertThatCode(() -> {
                metalstormClient.close();
                metalstormClient.close(); // Second call should be safe
            }).doesNotThrowAnyException();
        }
    }
}
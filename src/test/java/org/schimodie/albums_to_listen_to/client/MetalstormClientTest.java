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
import static org.schimodie.albums_to_listen_to.client.MetalstormClient.ALBUM_DATE_SELECTOR;
import static org.schimodie.albums_to_listen_to.client.MetalstormClient.ALBUM_ROWS_SELECTOR;

@ExtendWith(MockitoExtension.class)
class MetalstormClientTest {
    // Test constants
    private static final String BASE_URL = "https://metalstorm.net/";
    private static final String FILTER_URL = BASE_URL + "bands/albums.php?filter_show_invisible=1&show_invisible_submit=1";
    private static final String ALBUMS_PAGE_URL = BASE_URL + "bands/albums.php?page=";
    private static final String ALBUM_DETAIL_URL = BASE_URL + "bands/album.php?album_id=";
    private static final String TD_SELECTOR = "td";
    private static final String A_SELECTOR = "a";
    private static final String TEST_ARTIST = "Test Artist";
    private static final String TEST_ALBUM = "Test Album";
    private static final String ARTIST_ONE = "Artist One";
    private static final String ARTIST_TWO = "Artist Two";
    private static final String FULL_LENGTH = "Full-length";
    private static final String METAL = "Metal";
    private static final String INVALID = "invalid";
    private static final String BAND_HREF = "band.php?band_id=";
    private static final String ALBUM_HREF = "album.php?album_id=";
    
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
        when(mockRow.querySelectorAll(TD_SELECTOR)).thenReturn(columns);
        return mockRow;
    }

    private List<ElementHandle> createMockColumns() {
        // Create 8 columns as required by MetalstormClient
        return List.of(
                mock(ElementHandle.class), // column 0
                mock(ElementHandle.class), // column 1
                createMockArtistAlbumColumn(), // column 2 - artist/album
                mock(ElementHandle.class), // column 3
                createMockColumn(FULL_LENGTH), // column 4 - type
                createMockColumn(METAL), // column 5 - genre
                createMockRatingColumn(), // column 6 - rating
                createMockColumn("50") // column 7 - votes
        );
    }

    private ElementHandle createMockArtistAlbumColumn() {
        ElementHandle column = mock(ElementHandle.class);

        ElementHandle artistLink = mock(ElementHandle.class);
        when(artistLink.getAttribute("href")).thenReturn(BAND_HREF + "12345");
        when(artistLink.innerText()).thenReturn(TEST_ARTIST);

        ElementHandle albumLink = mock(ElementHandle.class);
        when(albumLink.getAttribute("href")).thenReturn(ALBUM_HREF + "67890");
        when(albumLink.innerText()).thenReturn(TEST_ALBUM);

        when(column.querySelectorAll(A_SELECTOR)).thenReturn(List.of(artistLink, albumLink));
        return column;
    }

    private ElementHandle createMockRatingColumn() {
        ElementHandle column = mock(ElementHandle.class);
        ElementHandle ratingLink = mock(ElementHandle.class);
        when(ratingLink.innerText()).thenReturn("8.5");
        when(column.querySelectorAll(A_SELECTOR)).thenReturn(List.of(ratingLink));
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
        when(mockRow.querySelectorAll(TD_SELECTOR)).thenReturn(insufficientColumns);
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
        when(mockRow.querySelectorAll(TD_SELECTOR)).thenReturn(columns);
        return mockRow;
    }

    private ElementHandle createMockColumnWithOnlyArtist() {
        ElementHandle column = mock(ElementHandle.class);
        ElementHandle artistLink = mock(ElementHandle.class);
        when(artistLink.getAttribute("href")).thenReturn(BAND_HREF + "12345");
        when(artistLink.innerText()).thenReturn(TEST_ARTIST);
        when(column.querySelectorAll(A_SELECTOR)).thenReturn(List.of(artistLink));
        return column;
    }

    private ElementHandle createMockRowWithInvalidRating() {
        ElementHandle mockRow = mock(ElementHandle.class);
        List<ElementHandle> columns = List.of(
                mock(ElementHandle.class), mock(ElementHandle.class), createMockArtistAlbumColumn(),
                mock(ElementHandle.class), createMockColumn(FULL_LENGTH), createMockColumn(METAL),
                createMockInvalidRatingColumn(), createMockColumn("50")
        );
        when(mockRow.querySelectorAll(TD_SELECTOR)).thenReturn(columns);
        return mockRow;
    }

    private ElementHandle createMockInvalidRatingColumn() {
        ElementHandle column = mock(ElementHandle.class);
        ElementHandle ratingLink = mock(ElementHandle.class);
        when(ratingLink.innerText()).thenReturn(INVALID);
        when(column.querySelectorAll(A_SELECTOR)).thenReturn(List.of(ratingLink));
        return column;
    }

    private ElementHandle createMockRowWithInvalidVotes() {
        ElementHandle mockRow = mock(ElementHandle.class);
        List<ElementHandle> columns = List.of(
                mock(ElementHandle.class), mock(ElementHandle.class), createMockArtistAlbumColumn(),
                mock(ElementHandle.class), createMockColumn(FULL_LENGTH), createMockColumn(METAL),
                createMockRatingColumn(), createMockColumn(INVALID)
        );
        when(mockRow.querySelectorAll(TD_SELECTOR)).thenReturn(columns);
        return mockRow;
    }

    private ElementHandle createMockAlbumRowWithMultipleArtists() {
        ElementHandle mockRow = mock(ElementHandle.class);
        List<ElementHandle> columns = List.of(
                mock(ElementHandle.class), // column 0
                mock(ElementHandle.class), // column 1
                createMockArtistAlbumColumnWithMultipleArtists(), // column 2 - multiple artists/album
                mock(ElementHandle.class), // column 3
                createMockColumn(FULL_LENGTH), // column 4 - type
                createMockColumn(METAL), // column 5 - genre
                createMockRatingColumn(), // column 6 - rating
                createMockColumn("50") // column 7 - votes
        );
        when(mockRow.querySelectorAll(TD_SELECTOR)).thenReturn(columns);
        return mockRow;
    }

    private ElementHandle createMockArtistAlbumColumnWithMultipleArtists() {
        ElementHandle column = mock(ElementHandle.class);

        ElementHandle artist1Link = mock(ElementHandle.class);
        when(artist1Link.getAttribute("href")).thenReturn(BAND_HREF + "11111");
        when(artist1Link.innerText()).thenReturn(ARTIST_ONE);

        ElementHandle artist2Link = mock(ElementHandle.class);
        when(artist2Link.getAttribute("href")).thenReturn(BAND_HREF + "22222");
        when(artist2Link.innerText()).thenReturn(ARTIST_TWO);

        ElementHandle albumLink = mock(ElementHandle.class);
        when(albumLink.getAttribute("href")).thenReturn(ALBUM_HREF + "67890");
        when(albumLink.innerText()).thenReturn(TEST_ALBUM);

        when(column.querySelectorAll(A_SELECTOR)).thenReturn(List.of(artist1Link, artist2Link, albumLink));
        return column;
    }

    private ElementHandle createMockAlbumRowWithZeroValues() {
        ElementHandle mockRow = mock(ElementHandle.class);
        List<ElementHandle> columns = List.of(
                mock(ElementHandle.class), // column 0
                mock(ElementHandle.class), // column 1
                createMockArtistAlbumColumn(), // column 2 - artist/album
                mock(ElementHandle.class), // column 3
                createMockColumn(FULL_LENGTH), // column 4 - type
                createMockColumn(METAL), // column 5 - genre
                createMockZeroRatingColumn(), // column 6 - rating
                createMockColumn("0") // column 7 - votes
        );
        when(mockRow.querySelectorAll(TD_SELECTOR)).thenReturn(columns);
        return mockRow;
    }

    private ElementHandle createMockZeroRatingColumn() {
        ElementHandle column = mock(ElementHandle.class);
        ElementHandle ratingLink = mock(ElementHandle.class);
        when(ratingLink.innerText()).thenReturn("0.0");
        when(column.querySelectorAll(A_SELECTOR)).thenReturn(List.of(ratingLink));
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
            when(mockPage.querySelectorAll(ALBUM_ROWS_SELECTOR)).thenReturn(List.of(mockRow));

            // When
            List<Album> albums = metalstormClient.getAlbums(pageNumber);

            // Then
            assertThat(albums).hasSize(1);
            Album album = albums.getFirst();
            assertThat(album.getAlbum()).isEqualTo(TEST_ALBUM);
            assertThat(album.getArtists()).isEqualTo(List.of(TEST_ARTIST));
            assertThat(album.getGenre()).isEqualTo(METAL);
            assertThat(album.getRating()).isCloseTo(8.5, withinPercentage(0.1));
            assertThat(album.getVotes()).isEqualTo(50);

            verify(mockPage).navigate(ALBUMS_PAGE_URL + pageNumber);
        }

        @Test
        @DisplayName("should handle empty page gracefully")
        void testGetAlbumsHandlesEmptyPage() {
            // Given
            int pageNumber = 999;
            when(mockPage.querySelectorAll(ALBUM_ROWS_SELECTOR)).thenReturn(List.of());

            // When
            List<Album> albums = metalstormClient.getAlbums(pageNumber);

            // Then
            assertThat(albums).isEmpty();
        }

        @Test
        @DisplayName("should set filter on first call")
        void testGetAlbumsSetFilterOnFirstCall() {
            // Given
            when(mockPage.querySelectorAll(ALBUM_ROWS_SELECTOR)).thenReturn(List.of());

            // When
            metalstormClient.getAlbums(1);

            // Then - verify filter URL was called first
            verify(mockPage).navigate(FILTER_URL);
            verify(mockPage).navigate(ALBUMS_PAGE_URL + "1");
        }

        @Test
        @DisplayName("should handle multiple albums on a page")
        void testGetAlbumsHandlesMultipleAlbums() {
            // Given
            int pageNumber = 1;
            ElementHandle mockRow1 = createMockAlbumRow();
            ElementHandle mockRow2 = createMockAlbumRow();
            when(mockPage.querySelectorAll(ALBUM_ROWS_SELECTOR)).thenReturn(List.of(mockRow1, mockRow2));

            // When
            List<Album> albums = metalstormClient.getAlbums(pageNumber);

            // Then
            assertThat(albums).hasSize(2);
            albums.forEach(album -> {
                assertThat(album.getAlbum()).isEqualTo(TEST_ALBUM);
                assertThat(album.getArtists()).isEqualTo(List.of(TEST_ARTIST));
                assertThat(album.getGenre()).isEqualTo(METAL);
                assertThat(album.getRating()).isCloseTo(8.5, withinPercentage(0.1));
                assertThat(album.getVotes()).isEqualTo(50);
            });

            verify(mockPage).navigate(ALBUMS_PAGE_URL + pageNumber);
        }

        @Test
        @DisplayName("should handle invalid row structure gracefully")
        void testGetAlbumsHandlesInvalidRowStructure() {
            // Given
            ElementHandle mockRow = createMockRowWithInsufficientColumns();
            when(mockPage.querySelectorAll(ALBUM_ROWS_SELECTOR)).thenReturn(List.of(mockRow));

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
            when(mockPage.querySelectorAll(ALBUM_ROWS_SELECTOR)).thenReturn(List.of(mockRow));

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
            when(mockPage.querySelectorAll(ALBUM_ROWS_SELECTOR)).thenReturn(List.of(mockRow));

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
            when(mockPage.querySelectorAll(ALBUM_ROWS_SELECTOR)).thenReturn(List.of(mockRow));

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
            when(mockPage.querySelectorAll(ALBUM_ROWS_SELECTOR)).thenReturn(List.of(mockRow));

            // Expected Album using Album.builder() with all fields
            Album expectedAlbum = Album.builder()
                    .album(TEST_ALBUM)
                    .artists(List.of(TEST_ARTIST))
                    .artistIds(List.of("12345"))
                    .albumId("67890")
                    .genre(METAL)
                    .type(FULL_LENGTH)
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

            verify(mockPage).navigate(ALBUMS_PAGE_URL + pageNumber);
        }

        @Test
        @DisplayName("should handle albums with multiple artists")
        void testGetAlbumsWithMultipleArtists() {
            // Given
            int pageNumber = 1;
            ElementHandle mockRow = createMockAlbumRowWithMultipleArtists();
            when(mockPage.querySelectorAll(ALBUM_ROWS_SELECTOR)).thenReturn(List.of(mockRow));

            // When
            List<Album> albums = metalstormClient.getAlbums(pageNumber);

            // Then
            assertThat(albums).hasSize(1);
            Album album = albums.getFirst();
            assertThat(album.getArtists()).containsExactly(ARTIST_ONE, ARTIST_TWO);
            assertThat(album.getArtistIds()).containsExactly("11111", "22222");
            assertThat(album.getAlbumId()).isEqualTo("67890");
        }

        @Test
        @DisplayName("should handle albums with zero votes and rating")
        void testGetAlbumsWithZeroVotesAndRating() {
            // Given
            int pageNumber = 1;
            ElementHandle mockRow = createMockAlbumRowWithZeroValues();
            when(mockPage.querySelectorAll(ALBUM_ROWS_SELECTOR)).thenReturn(List.of(mockRow));

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
            when(mockPage.querySelector(ALBUM_DATE_SELECTOR))
                    .thenReturn(mockDateElement);

            // When
            Instant result = metalstormClient.getAlbumDate(albumId);

            // Then
            Instant expected = Instant.parse("2024-11-15T00:00:00Z");
            assertThat(result).isEqualTo(expected);
            verify(mockPage).navigate(ALBUM_DETAIL_URL + albumId);
        }

        @Test
        @DisplayName("should throw exception when date element is null")
        void testGetAlbumDateThrowsOnNullElement() {
            // Given
            String albumId = "12345";
            when(mockPage.querySelector(anyString())).thenReturn(null);

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

            // When
            Instant result = metalstormClient.getAlbumDate(albumId);

            // Then
            assertThat(result).isNotNull();
            verify(mockPage).navigate(ALBUM_DETAIL_URL + albumId);
        }

        @Test
        @DisplayName("should return correct Instant for album date")
        void testGetAlbumDateReturnsCorrectInstant() {
            // Given
            String albumId = "12345";
            ElementHandle mockDateElement = mock(ElementHandle.class);
            when(mockDateElement.innerText()).thenReturn("25 December 2023");
            when(mockPage.querySelector(anyString())).thenReturn(mockDateElement);

            // When
            Instant result = metalstormClient.getAlbumDate(albumId);

            // Then
            Instant expected = Instant.parse("2023-12-25T00:00:00Z");
            assertThat(result)
                    .isNotNull()
                    .isEqualTo(expected);

            // Verify the correct navigation occurred
            verify(mockPage).navigate(ALBUM_DETAIL_URL + albumId);
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
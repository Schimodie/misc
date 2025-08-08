# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Java 24 Maven project that downloads, filters, and manages album data from Metalstorm and LastFM APIs. The
main functionality includes web scraping album information, filtering based on criteria, and storing results locally in
JSON format.

## Common Development Commands

### Build and Compile

```bash
mvn clean compile
```

### Run Tests

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=AlbumListTest

# Run a specific test method
mvn test -Dtest=StorageFileNameTest#testCreateFileName
```

### Package the Application

```bash
mvn package
```

### Run the Main Application

```bash
mvn exec:java -Dexec.mainClass="org.schimodie.albums_to_listen_to.Main" -Dexec.args="--enable-preview"

# Or if using IntelliJ IDEA run configuration
# Run configuration "Main" with --enable-preview flag
```

## Architecture Overview

### Package Structure

- **org.schimodie.albums_to_listen_to** - Core album management module
    - `Main.java` - Application entry point that orchestrates the download, filter, and storage workflow
    - `AlbumsDownloader` - Downloads albums from Metalstorm API after a specified date
    - `AlbumsFilter` - Filters albums into good/bad categories based on criteria
    - `AlbumList` - Manages and sorts album collections by votes and priority
    - **client/** - Web scraping clients
        - `MetalstormClient` - JSoup-based scraper for Metalstorm with retry logic and cookie management
        - `LastFMClient` - Client for LastFM API integration
    - **database/** - Persistence layer
        - `Storage` - Handles JSON serialization/deserialization of albums to/from local files
        - `StorageFileName` - Utility for generating standardized file names
    - **bean/** - Data models
        - `Album` - Main album entity (uses Lombok @Builder)
        - `Pair` - Simple key-value pair structure

- **org.schimodie.common** - Shared utilities
    - **data/** - Generic data structures
        - `Tuple2`, `Tuple3`, `Tuple4` - Immutable tuple implementations
    - **utils/** - Utility classes
        - `Retryable` - Interface for retry logic
        - `ExponentialBackoffRetryable` - Exponential backoff retry strategy implementation
        - `RetryableException` - Custom exception for retry operations

- **org.schimodie.random** - Random ordering functionality
    - `RandomDailyOrder` - Generates random daily orderings

- **org.schimodie.stats** - Statistical simulations
    - `GamblingSim` - Gambling simulation with investment strategies

### Data Flow

1. **Download Phase**: `AlbumsDownloader` fetches album data from Metalstorm API for albums after a specified date
2. **Filter Phase**: `AlbumsFilter` processes albums and categorizes them as good or bad
3. **Sort Phase**: `AlbumList` sorts albums by votes and priority (high/low)
4. **Storage Phase**: `Storage` persists filtered albums to JSON files in the `db/` directory

### Key Technical Details

- **Java Version**: Java 24 with preview features enabled
- **Build Tool**: Maven
- **Dependencies**:
    - Jackson (2.19.2) - JSON processing
    - JSoup (1.21.1) - HTML parsing and web scraping
    - Lombok (1.18.38) - Code generation (compile-time only)
    - JUnit Jupiter (5.13.4) - Unit testing
    - Jakarta XML Bind API (4.0.2) - XML binding

- **Data Storage**:
    - JSON files stored in `db/metalstorm-lastfm-spotify-playlist/` directory
    - File naming convention uses dates and descriptive prefixes via `StorageFileName` utility

- **API Integration**:
    - Metalstorm client includes retry logic with exponential backoff
    - Cookie management for session handling
    - Custom headers for API requests

- **Date Handling**: Uses `Instant` and custom date formatters for date-based filtering

## Testing

The project uses JUnit 5 for testing. Test files are located in `src/test/java/` with corresponding package structure.
Key test classes include:

- `AlbumListTest` - Tests for album list management
- `AlbumTest` - Tests for album entity
- `StorageFileNameTest` - Tests for file naming utilities

## Important Notes

- The application requires network access to fetch data from Metalstorm and LastFM APIs
- Preview features are required for Java 24 language features
- Data persistence uses the local file system under the `db/` directory
- The Metalstorm client implements cookie-based session management
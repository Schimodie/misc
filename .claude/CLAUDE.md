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
        - `MetalstormClient` - Playwright-based browser automation for Metalstorm with retry logic, manages Browser/BrowserContext lifecycle, CSS selectors as constants
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
    - Jackson JSR310 (2.19.2) - Java time/date serialization
    - Playwright (1.40.0) - Browser automation for web scraping
    - JSoup (1.21.1) - HTML parsing
    - Lombok (1.18.38) - Code generation (compile-time only)
    - JUnit Jupiter (5.13.4) - Unit testing
    - AssertJ (3.27.3) - Fluent assertion library (test only)
    - Mockito (5.18.0) - Mocking framework with JUnit 5 integration (test only)
    - Jakarta XML Bind API (4.0.2) - XML binding

- **Data Storage**:
    - JSON files stored in `db/metalstorm-lastfm-spotify-playlist/` directory
    - File naming convention uses dates and descriptive prefixes via `StorageFileName` utility

- **Web Scraping**:
    - Playwright for browser automation (headless Chromium)
    - Dynamic content handling with automatic wait strategies
    - CSS selectors extracted as constants for maintainability
    - Browser context manages session state and cookies

- **API Integration**:
    - Metalstorm client uses Playwright browser automation with retry logic and exponential backoff
    - Browser context manages cookies/session automatically
    - CSS selectors for dynamic content extraction
    - Custom headers for API requests

- **Date Handling**: Uses `Instant` and custom date formatters for date-based filtering

## Testing

The project uses JUnit 5 with AssertJ assertions and Mockito for mocking. Test files are located in `src/test/java/` with corresponding package structure.
Key test classes include:

- `AlbumListTest` - Tests for album list management
- `AlbumTest` - Tests for album entity
- `StorageFileNameTest` - Tests for file naming utilities
- `MetalstormClientTest` - Tests for Playwright browser automation, album parsing, and date extraction

## Code Style Guidelines

### Import Conventions
- **Always use fully qualified imports** - Never use wildcard imports (e.g., `import java.util.*;`)
- **Import individual classes explicitly** - Each import should specify exactly one class
- **Static imports must be fully qualified** - Static methods and fields should be imported individually
- Example:
  ```java
  // Good
  import java.util.List;
  import java.util.Map;
  import static java.util.stream.Collectors.toList;
  
  // Bad
  import java.util.*;
  import static java.util.stream.Collectors.*;
  ```

## Important Notes

- The application requires network access to fetch data from Metalstorm and LastFM APIs
- Preview features are required for Java 24 language features
- Data persistence uses the local file system under the `db/` directory
- Playwright automatically downloads Chromium browser binaries on first run
- Web scraping runs in headless browser mode (no GUI required)
- Browser lifecycle is managed via AutoCloseable pattern

## Startup Checklist

MUST execute on project activation: Run `bash ls -la .claude/` to scan .claude (NEVER use LS tool for .claude)

## File Exploration Directives

ALWAYS follow this tool hierarchy:

### For Code Navigation:

- **Java files**: MUST use Serena `find_symbol` with name_path, NEVER read entire files
- **Finding classes/methods**: Serena `get_symbols_overview` → `find_symbol` with depth=1 → `include_body=true`
- **Finding references**: Serena `find_referencing_symbols` instead of grep
- **Pattern search in code**: Serena `search_for_pattern` with `restrict_search_to_code_files=true`

### For File Discovery:

- **Pattern matching**: Glob for `*.java`, `**/*.xml` patterns
- **Directory listing**: Bash `ls -la` for all files
- **Config files** (pom.xml, properties): Read directly, don't use Serena

### NEVER:

- Use LS on .claude directory
- Read entire Java files when symbols suffice
- Use grep/find in Bash when Serena/Glob available

## MCP Tool Directives

### Semgrep (Security)

- ALWAYS run `security_check` BEFORE commits
- MUST run when: adding dependencies, creating endpoints, handling uploads, database operations
- Run `semgrep_scan` with config="p/security" for comprehensive checks

### Context7 (Documentation)

- ALWAYS use BEFORE implementing Jackson serialization
- MUST use when: working with JSoup selectors, Lombok annotations, JUnit 5 features
- To `get-library-docs` you MUST check @.claude/library-ids.md for library-ids; if missing:
    - `resolve-library-id` then append `{library}: {library-id}` to the file

### IntelliJ IDEA (IDE Operations)

- PREFER `execute_run_configuration` over `mvn test` for single test runs
- MUST use `rename_refactoring` for symbol renames (not find-replace)
- USE `get_file_problems` before commits to catch compilation errors
- USE `get_project_modules` when exploring project structure

### GitHub (Repository)

- ALWAYS use for: PRs (`create_pull_request`), issues (`create_issue`), reviews
- MUST use `search_code` for cross-repo searches, not local grep
- USE `list_workflow_runs` to check CI status after pushes

### Serena (Semantic Analysis)

- PRIMARY tool for Java code exploration
- ALWAYS use instead of Read for understanding code structure
- MUST use `replace_symbol_body` for method rewrites
- USE `insert_after_symbol`/`insert_before_symbol` for adding methods/imports

## Testing & Security Checklist

### Before ANY Code Changes:

1. Run `mvn test` to ensure baseline passes
2. Use IntelliJ `get_file_problems` on target files

### After Code Changes:

1. Run Semgrep `security_check` on modified files
2. Execute `mvn test -Dtest=<AffectedTestClass>`
3. Run `mvn compile` to catch compilation errors

### Before Commits:

1. MUST run full `mvn test`
2. MUST run Semgrep security scan
3. Check IntelliJ `get_project_problems` for warnings
4. Verify no secrets in code (search for "password", "key", "token")
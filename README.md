# YouTube Data CLI

A command-line tool to fetch data from YouTube using the YouTube Data API v3. It supports OAuth2 authentication, stores a refresh token, and allows extensible data requests with centralized storage.

## Features
- Automatic refresh token creation and storage (skipped if already present)
- Menu-driven CLI for various YouTube data requests
- Extensible fetcher interface for adding new data requests
- Centralized file-based storage (easy to swap for other backends)
- Handles pagination and error cases

## Prerequisites
- Java 21+
- Maven
- Google Cloud project with YouTube Data API v3 enabled

## Setup
1. **Clone the repository:**
   ```sh
   git clone <your-repo-url>
   cd youtube-data-cli
   ```
2. **Create credentials:**
   - Go to [Google Cloud Console](https://console.cloud.google.com/apis/credentials)
   - Create OAuth 2.0 Client ID (Desktop app)
   - Download the `client_secret.json` file
   - Place it in `credentials/client_secret.json`
3. **Build the project:**
   ```sh
   mvn clean package
   ```
4. **Run the CLI:**
   ```sh
   java -jar target/youtube-data-cli-1.0-SNAPSHOT.jar
   ```
   - On first run, follow the browser-based OAuth flow. The refresh token will be saved in `tokens/refresh_token.json`.
   - On subsequent runs, the tool will use the saved refresh token.

## Adding New Data Fetchers
- Implement the `DataFetcher` interface in `src/main/java/org/bgf/youtube/fetcher/`.
- Add your fetcher to the menu in `Main.java`.
- Use `StorageManager` for all data persistence.

## Storage
- All fetched data is stored as JSON in the `data/` directory by default.
- To change storage backend, modify or replace `StorageManager`.

## License
MIT

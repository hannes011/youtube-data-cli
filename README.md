# YouTube Data CLI

A command-line tool to fetch data from YouTube using the YouTube Data API v3. It supports multi-account OAuth2 authentication, stores refresh tokens with account information, and allows extensible data requests with centralized storage.

## Features
- **Multi-account OAuth2 authentication** with account selection menu
- **Two authentication methods**: Manual authorization code or browser-based callback
- **Automatic refresh token management** with channel name association
- **Account switching** without restarting the CLI
- **Menu-driven CLI** for various YouTube data requests
- **Extensible fetcher interface** for adding new data requests
- **Centralized file-based storage** (easy to swap for other backends)
- **Robust error handling** including quota management and rate limiting
- **Pagination support** for large datasets

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

## Authentication & Account Management

### First Run
On first run, you'll be prompted to authenticate:
1. Choose authentication method:
   - **Via authentication code**: Copy-paste authorization code from browser
   - **Via direct authentication**: Browser opens automatically for OAuth flow
2. The tool will fetch your channel name and store the refresh token

### Multi-Account Support
- All refresh tokens are stored in `tokens/refresh_tokens.json`
- Each token is associated with the channel name for easy identification
- On startup, select from existing accounts or add a new one
- Use "Change account" from the main menu to switch accounts without restarting

### Available Data Fetchers
- **Video Details**: Fetch details for all your videos or specific video IDs
- **Channel List**: List channels for your account or other accounts
- **Playlist Details**: Get playlists and their videos for a channel
- **Recent Changes**: Find recent uploads and changes since a specified date
- **Subscriber Counts**: Get subscriber statistics for channel IDs

## Adding New Data Fetchers
- Implement the `DataFetcher` interface in `src/main/java/org/bgf/youtube/fetcher/`
- Add your fetcher to the menu in `Main.java`
- Use `StorageManager` for all data persistence
- Follow the existing patterns for error handling and quota management

## Storage
- All fetched data is stored as JSON in the `data/` directory by default
- Thumbnails are downloaded and stored with proper file extensions
- To change storage backend, modify or replace `StorageManager`

## File Structure
```
├── credentials/
│   └── client_secret.json          # OAuth2 credentials
├── tokens/
│   └── refresh_tokens.json         # Multi-account refresh tokens
├── data/                           # Fetched data and thumbnails
└── src/main/java/org/bgf/youtube/
    ├── auth/                       # Authentication management
    ├── fetcher/                    # Data fetching implementations
    └── storage/                    # Storage management
```

## License
Apache 2.0

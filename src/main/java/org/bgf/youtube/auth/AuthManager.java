package org.bgf.youtube.auth;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.ArrayList;
import org.bgf.youtube.PromptService;

public class AuthManager {
    private static final String CREDENTIALS_FILE = "credentials/client_secrets.json";
    private static final String REFRESH_TOKENS_FILE = "tokens/refresh_tokens.json";
    private static final String TOKENS_DIR = "tokens";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = List.of("https://www.googleapis.com/auth/youtube.readonly", "https://www.googleapis.com/auth/yt-analytics.readonly");
    private static final Logger logger = LoggerFactory.getLogger(AuthManager.class);
    private static final Gson gson = new Gson();

    public static class RefreshTokenEntry {
        public String refresh_token;
        public String channel_name;
    }

    public Credential authorize() throws GeneralSecurityException {
        try {
            var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            File credFile = new File(CREDENTIALS_FILE);
            if (!credFile.exists()) {
                logger.error("Credentials file not found: {}", CREDENTIALS_FILE);
                return null;
            }
            try (InputStream in = new FileInputStream(credFile)) {
                var clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
                var flow = new GoogleAuthorizationCodeFlow.Builder(
                        httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                        .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIR)))
                        .setAccessType("offline")
                        .build();

                // 1. Load all refresh tokens
                List<RefreshTokenEntry> tokens = loadRefreshTokens();

                // 2. Let user select an account or choose to add a new one
                RefreshTokenEntry selected = selectAccount(tokens);
                if (selected != null) {
                    return useExistingToken(flow, selected);
                }

                // 3. New account: prompt for authentication method
                String method = promptAuthMethod();
                Credential credential = authenticate(flow, method);
                if (credential != null && credential.getRefreshToken() != null) {
                    credential.refreshToken();
                    // 4. Fetch channel name for the new account
                    String channelName = fetchChannelName(httpTransport, credential);
                    // 5. Save new token
                    saveNewToken(tokens, credential.getRefreshToken(), channelName);
                }
                return credential;
            }
        } catch (IOException e) {
            logger.error("Failed to authorize with Google OAuth: {}", e.getMessage(), e);
            return null;
        } catch (Exception e) {
            logger.error("Unexpected error during authorization: {}", e.getMessage(), e);
            return null;
        }
    }

    // Loads all refresh tokens from file
    private List<RefreshTokenEntry> loadRefreshTokens() {
        File file = new File(REFRESH_TOKENS_FILE);
        if (!file.exists()) return new ArrayList<>();
        try (Reader reader = new FileReader(file)) {
            RefreshTokenEntry[] arr = gson.fromJson(reader, RefreshTokenEntry[].class);
            if (arr == null) return new ArrayList<>();
            return new ArrayList<>(List.of(arr));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // Presents a menu to select an account or choose to add a new one
    private RefreshTokenEntry selectAccount(List<RefreshTokenEntry> tokens) {
        if (tokens.isEmpty()) return null;
        List<String> options = new ArrayList<>();
        for (var t : tokens) options.add("Use @" + t.channel_name);
        options.add("Use a new account");
        String selectedName = PromptService.promptMenu("Which account do you want to use?", options, null);
        if (selectedName != null && !selectedName.equals("Use a new account")) {
            selectedName = selectedName.substring(5);
            for (var t : tokens) {
                if (t.channel_name.equals(selectedName)) return t;
            }
        }
        return null;
    }

    // Uses an existing refresh token to create a Credential
    private Credential useExistingToken(GoogleAuthorizationCodeFlow flow, RefreshTokenEntry entry) throws IOException {
        var stored = new StoredCredential();
        stored.setRefreshToken(entry.refresh_token);
        flow.getCredentialDataStore().set("user", stored);
        var credential = flow.loadCredential("user");
        if (credential != null) credential.refreshToken();
        return credential;
    }

    // Prompts the user for authentication method
    private String promptAuthMethod() {
        return PromptService.promptMenu("How do you want to authenticate?", List.of("Via authentication code", "Via direct authentication (browser callback)"), null);
    }

    // Handles authentication (manual code or browser callback)
    private Credential authenticate(GoogleAuthorizationCodeFlow flow, String method) throws IOException {
        if (method.equals("Via direct authentication (browser callback)")) {
            var receiver = new com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver.Builder().setPort(8888).build();
            // Remove any existing credential for 'user' to avoid session reuse
            flow.getCredentialDataStore().delete("user");
            return new com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        } else {
            var authorizationUrl = flow.newAuthorizationUrl().setRedirectUri("urn:ietf:wg:oauth:2.0:oob").build();
            System.out.println("No credentials found. Please authenticate via the following URL:");
            System.out.println(authorizationUrl);
            String code = PromptService.prompt("Enter the authorization code: ");
            var tokenResponse = flow.newTokenRequest(code)
                    .setRedirectUri("urn:ietf:wg:oauth:2.0:oob")
                    .execute();
            return flow.createAndStoreCredential(tokenResponse, "user");
        }
    }

    // Fetches the channel name for the authenticated account
    private String fetchChannelName(com.google.api.client.http.HttpTransport httpTransport, Credential credential) {
        try {
            var youtube = new YouTube.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName("YouTubeDataCli").build();
            var channelReq = youtube.channels().list(List.of("snippet")).setMine(true);
            var channelResp = channelReq.execute();
            if (channelResp.getItems() != null && !channelResp.getItems().isEmpty()) {
                var snippet = channelResp.getItems().get(0).getSnippet();
                return snippet.getTitle();
            }
        } catch (Exception e) {
            logger.error("Failed to fetch channel name: {}", e.getMessage(), e);
        }
        return "unknown";
    }

    // Saves a new refresh token to the file
    private void saveNewToken(List<RefreshTokenEntry> tokens, String refreshToken, String channelName) {
        RefreshTokenEntry entry = new RefreshTokenEntry();
        entry.refresh_token = refreshToken;
        entry.channel_name = channelName;
        tokens.add(entry);
        saveRefreshTokens(tokens);
        logger.info("Refresh token saved to {}", REFRESH_TOKENS_FILE);
    }

    // Writes the list of tokens to file
    private void saveRefreshTokens(List<RefreshTokenEntry> tokens) {
        try (Writer writer = new FileWriter(REFRESH_TOKENS_FILE)) {
            gson.toJson(tokens, writer);
        } catch (IOException e) {
            logger.error("Failed to save refresh tokens: {}", e.getMessage(), e);
        }
    }
}
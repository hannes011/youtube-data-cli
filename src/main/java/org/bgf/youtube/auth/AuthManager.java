package org.bgf.youtube.auth;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Scanner;

public class AuthManager {
    private static final String CREDENTIALS_FILE = "credentials/client_secrets.json";
    private static final String TOKENS_DIR = "tokens";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = List.of("https://www.googleapis.com/auth/youtube.readonly");
    private static final Logger logger = LoggerFactory.getLogger(AuthManager.class);
    private static final String REFRESH_TOKEN_FILE = "tokens/refresh_token.json";
    private static final Gson gson = new Gson();

    public static Credential authorize() throws GeneralSecurityException {
        // Check for existing refresh token file
        File refreshTokenFile = new File(REFRESH_TOKEN_FILE);
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
                if (refreshTokenFile.exists()) {
                    try (Reader reader = new FileReader(refreshTokenFile)) {
                        var map = gson.fromJson(reader, java.util.Map.class);
                        if (map != null && map.get("refresh_token") != null) {
                            logger.info("Refresh token found, skipping OAuth flow.");
                            // Try to load credential from the flow's store
                            var credential = flow.loadCredential("user");
                            if (credential == null || credential.getRefreshToken() == null) {
                                // Store the refresh token in the credential store
                                var stored = new com.google.api.client.auth.oauth2.StoredCredential();
                                stored.setRefreshToken((String) map.get("refresh_token"));
                                flow.getCredentialDataStore().set("user", stored);
                                credential = flow.loadCredential("user");
                            }
                            if (credential != null) credential.refreshToken();
                            return credential;
                        }
                    } catch (Exception e) {
                        logger.error("Failed to load refresh token: {}", e.getMessage(), e);
                        // Fall through to OAuth flow
                    }
                }
                // TODO // If not found, run OAuth flow and save refresh token
                // var receiver = new LocalServerReceiver.Builder().setPort(8888).build();
                // var credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
                // If not found, run manual OAuth flow and save refresh token
                System.out.println("No credentials found. Please authenticate via the following URL:");
                var authorizationUrl = flow.newAuthorizationUrl().setRedirectUri("urn:ietf:wg:oauth:2.0:oob").build();
                System.out.println(authorizationUrl);
                System.out.print("Enter the authorization code: ");
                Scanner scanner = new Scanner(System.in);
                String code = scanner.nextLine();
                var tokenResponse = flow.newTokenRequest(code)
                        .setRedirectUri("urn:ietf:wg:oauth:2.0:oob")
                        .execute();
                var credential = flow.createAndStoreCredential(tokenResponse, "user");
                if (credential.getRefreshToken() != null) {
                    credential.refreshToken();
                    // Save refresh token to file
                    try (Writer writer = new FileWriter(REFRESH_TOKEN_FILE)) {
                        gson.toJson(java.util.Map.of("refresh_token", credential.getRefreshToken()), writer);
                        logger.info("Refresh token saved to {}", REFRESH_TOKEN_FILE);
                    } catch (IOException e) {
                        logger.error("Failed to save refresh token: {}", e.getMessage(), e);
                    }
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
}
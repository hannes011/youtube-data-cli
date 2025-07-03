package org.bgf.youtube.auth;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.List;

public class AuthManager {
    private static final String CREDENTIALS_FILE = "credentials/client_secret.json";
    private static final String TOKENS_DIR = "tokens";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final List<String> SCOPES = List.of("https://www.googleapis.com/auth/youtube.readonly");

    public static Credential authorize() throws IOException, GeneralSecurityException {
        var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        try (InputStream in = new FileInputStream(CREDENTIALS_FILE)) {
            var clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
            var flow = new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIR)))
                    .setAccessType("offline")
                    .build();
            var receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            var credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
            // Force refresh if needed
            if (credential.getRefreshToken() != null) credential.refreshToken();
            return credential;
        }
    }
}
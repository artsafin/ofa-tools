package com.artsafin.ofa.utils.google;

import com.artsafin.ofa.Main;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

public class GoogleServiceFactory {
    private final String appName;
    private NetHttpTransport googleHttp = GoogleNetHttpTransport.newTrustedTransport();

    private static final JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Arrays.asList(SheetsScopes.SPREADSHEETS, DriveScopes.DRIVE);
    private static final String CREDENTIALS_FILE_PATH = "/client_id.json";
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    public GoogleServiceFactory(String appName) throws GeneralSecurityException, IOException {
        this.appName = appName;
    }

    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = Main.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(jsonFactory, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, jsonFactory, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    Sheets createSheetsService() throws IOException {
        return new Sheets.Builder(googleHttp, jsonFactory, getCredentials(googleHttp))
                .setApplicationName(appName)
                .build();
    }

    Drive createDriveService() throws IOException {
        return new Drive.Builder(googleHttp, jsonFactory, getCredentials(googleHttp))
                .setApplicationName(appName)
                .build();
    }
}

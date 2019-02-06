package com.artsafin.ofa.utils.airtable;

import java.io.IOException;

public class AirtableTable<T> {
    private final AirtableApiClient.Query url;
    private final AirtableApiClient apiClient;

    public AirtableTable(AirtableApiClient.Query url, AirtableApiClient apiClient) {
        this.url = url;
        this.apiClient = apiClient;
    }

    public <A extends AirtableRecords<T>> A getRecords(Class<A> parseAs) throws AirtableLoadException {
        try {
            return apiClient.get(url.build()).parseAs(parseAs);
        } catch (IOException | InvalidAirtableUrlException e) {
            throw new AirtableLoadException(e);
        }
    }

    public <A extends AirtableRecord<T>> A getRecord(String id, Class<A> parseAs) throws AirtableLoadException {
        try {
            return apiClient.get(url.id(id).build()).parseAs(parseAs);
        } catch (IOException | InvalidAirtableUrlException e) {
            throw new AirtableLoadException(e);
        }
    }
}

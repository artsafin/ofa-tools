package com.artsafin.ofa.utils.airtable;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;

import javax.annotation.Nonnull;
import java.io.IOException;

public class AirtableApiClient {
    public static class Query implements Cloneable {
        private static String URL_PREFIX = "https://api.airtable.com/v0";
        private final String db;
        private String table;
        private String id;

        private GenericUrl url = new GenericUrl(URL_PREFIX);

        public Query(@Nonnull String db) {
            this.db = db;
        }

        @Override
        protected Query clone() throws CloneNotSupportedException {
            Query clone = (Query) super.clone();
            clone.url = new GenericUrl(URL_PREFIX);

            return clone;
        }

        public Query table(@Nonnull String table) {
            Query q = null;
            try {
                q = clone();
                q.table = table;
            } catch (CloneNotSupportedException ignore) {
            }
            return q;
        }

        public Query id(@Nonnull String id) {
            Query q = null;
            try {
                q = clone();
                q.id = id;
            } catch (CloneNotSupportedException ignore) {
            }
            return q;
        }

        public Query filterByFormula(@Nonnull String formula) {

            Query q = null;
            try {
                q = clone();
                q.url.set("filterByFormula", formula);
            } catch (CloneNotSupportedException ignore) {
            }
            return q;
        }

        GenericUrl build() throws InvalidAirtableUrlException {
            if (table == null) {
                throw new InvalidAirtableUrlException("Table cannot be empty");
            }

            GenericUrl url = this.url.clone();

            url.getPathParts().add(db);
            url.getPathParts().add(table);

            if (id != null) {
                url.getPathParts().add(id);
            }

            return url;
        }
    }

    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();

    private HttpRequestFactory requestFactory =
            HTTP_TRANSPORT.createRequestFactory(request -> request.setParser(new JsonObjectParser(JSON_FACTORY)));

    private HttpHeaders headers = new HttpHeaders();

    public AirtableApiClient(String apiKey) {
        headers.setAuthorization("Bearer " + apiKey);
    }

    public HttpResponse get(GenericUrl url) throws IOException {
        HttpRequest request = requestFactory
                .buildGetRequest(url)
                .setHeaders(headers);
        return request.execute();
    }
}

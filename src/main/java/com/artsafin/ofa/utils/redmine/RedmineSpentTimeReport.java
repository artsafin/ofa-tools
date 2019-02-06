package com.artsafin.ofa.utils.redmine;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.util.Key;

import java.io.IOException;
import java.util.List;

public class RedmineSpentTimeReport {
    private final String host;
    private final String apiKey;

    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory();

    public RedmineSpentTimeReport(String host, String apiKey) {
        this.host = host;
        this.apiKey = apiKey;
    }

    public String getHost() {
        return host;
    }

    public static class ReportParams {
        public final String userId;
        public final String from;
        public final String to;

        public ReportParams(String userId, String from, String to) {
            this.userId = userId;
            this.from = from;
            this.to = to;
        }
    }

    private class ReportUrl extends GenericUrl {
        @Key("user_id")
        private String userId;

        @Key("from")
        private String from;

        @Key("to")
        private String to;

        public ReportUrl(ReportParams params) {
            super(host + "/time_entries.csv");

            userId = params.userId;
            from = params.from;
            to = params.to;
        }
    }

    public List<SpentTimeEntry> loadEntries(ReportParams params) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Redmine-API-Key", apiKey);

        HttpRequest request = requestFactory
                .buildGetRequest(new ReportUrl(params))
                .setHeaders(headers);

        HttpResponse response = request.execute();

        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper.schemaFor(SpentTimeEntry.class).withSkipFirstDataRow(true);

        MappingIterator<SpentTimeEntry> it = mapper.readerFor(SpentTimeEntry.class).with(schema).readValues(response.getContent());
        return it.readAll();
    }
}

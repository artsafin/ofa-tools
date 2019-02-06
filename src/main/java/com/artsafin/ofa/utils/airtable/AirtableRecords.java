package com.artsafin.ofa.utils.airtable;

import com.google.api.client.util.Key;

import java.util.List;

public class AirtableRecords<RT> {
    @Key
    public List<AirtableRecord<RT>> records;

    @Key
    public String offset;
}

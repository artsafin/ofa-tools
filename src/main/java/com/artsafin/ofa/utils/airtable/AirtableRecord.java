package com.artsafin.ofa.utils.airtable;

import com.google.api.client.util.Key;

public class AirtableRecord<T> {
    @Key
    public String id;

    @Key
    public T fields;

    @Key
    public String createdTime;
}
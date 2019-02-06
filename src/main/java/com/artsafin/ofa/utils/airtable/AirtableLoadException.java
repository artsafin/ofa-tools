package com.artsafin.ofa.utils.airtable;

public class AirtableLoadException extends Exception {
    public AirtableLoadException(String message, Throwable cause) {
        super(message, cause);
    }

    public AirtableLoadException(Throwable cause) {
        super(cause);
    }
}

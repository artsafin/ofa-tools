package com.artsafin.ofa.app;

public class PayslipsLoadException extends Exception {
    private final Iterable<String> loadErrors;

    public PayslipsLoadException(Iterable<String> loadErrors) {
        this.loadErrors = loadErrors;
    }

    public String getErrors(String glue) {
        return String.join(glue, loadErrors);
    }
}

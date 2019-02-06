package com.artsafin.ofa.app;

public class InvoiceNotFoundException extends Exception {
    public final String invoiceIdPart;

    public InvoiceNotFoundException(String invoiceIdPart) {

        this.invoiceIdPart = invoiceIdPart;
    }
}

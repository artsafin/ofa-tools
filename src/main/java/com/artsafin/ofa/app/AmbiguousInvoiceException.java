package com.artsafin.ofa.app;

public class AmbiguousInvoiceException extends Exception {
    public final String invoiceIdPart;
    public final String ids;

    public AmbiguousInvoiceException(String invoiceIdPart, String ids) {
        this.invoiceIdPart = invoiceIdPart;
        this.ids = ids;
    }
}

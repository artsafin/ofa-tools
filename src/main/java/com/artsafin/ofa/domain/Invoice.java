package com.artsafin.ofa.domain;

import com.google.api.client.util.Key;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.StringJoiner;

import static java.time.format.DateTimeFormatter.ofPattern;

public class Invoice {
    @Key("No")
    public String id;

    @Key("Number")
    public int number;

    @Key("Invoice date")
    private String invoiceDate;

    @Key("Filename")
    public String filename;

    @Key("Hour rate")
    public double hourRate;

    @Key("Hours")
    public int hours;

    @Key("Total")
    public double total;

    @Key("Return of rounding")
    public double returnOfRounding;

    @Key("Hour Rate Rounding")
    public double rounding;

    private LocalDate invoiceDate() {
        return LocalDate.parse(invoiceDate, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public String invoiceDateIso() {
        return invoiceDate;
    }

    public String invoiceDateBeginIso() {
        return invoiceDate().minusMonths(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public String invoiceDateYmd() {
        return invoiceDate().format(ofPattern("LLLL dd YYYY", Locale.US));
    }

    public String invoiceDateYm() {
        return invoiceDate().format(ofPattern("LLLL YYYY", Locale.US));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Invoice.class.getSimpleName() + "[", "]")
                .add("id='" + id + "'")
                .add("number=" + number)
                .add("invoiceDate='" + invoiceDate + "'")
                .add("filename='" + filename + "'")
                .add("hourRate=" + hourRate)
                .add("hours=" + hours)
                .add("total=" + total)
                .add("returnOfRounding=" + returnOfRounding)
                .add("rounding=" + rounding)
                .toString();
    }
}

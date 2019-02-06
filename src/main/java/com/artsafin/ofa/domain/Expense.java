package com.artsafin.ofa.domain;

import com.google.api.client.util.Key;

import java.util.StringJoiner;

public class Expense {
    @Key("ID")
    public String id;

    @Key("Planned?")
    public boolean isPlanned;

    @Key("Plan comment")
    public String planComment;

    @Key("Subject")
    public String subject;

    @Key("Tag")
    public String tag;

    @Key("Amount, RUB")
    public double rubAmount;

    @Key("Amount, EUR")
    public double eurAmount;

    public String rubAmountRounded() {
        return Currency.formatTwoPlacesDot(rubAmount);
    }

    public String eurAmountRounded() {
        return Currency.formatTwoPlacesDot(eurAmount);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Expense.class.getSimpleName() + "[", "]")
                .add("id='" + id + "'")
                .add("isPlanned=" + isPlanned)
                .add("planComment=" + planComment)
                .add("subject='" + subject + "'")
                .add("tag='" + tag + "'")
                .add("rubAmount=" + rubAmount)
                .add("eurAmount=" + eurAmount)
                .toString();
    }
}

package com.artsafin.ofa.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class Currency {
    private static DecimalFormat twoPlacesFormatComma;
    private static DecimalFormat twoPlacesFormatDot;
    private static DecimalFormat wholeFormat;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');

        twoPlacesFormatDot = new DecimalFormat("#.##", symbols);
        setCommonParams(twoPlacesFormatDot);

        twoPlacesFormatComma = new DecimalFormat("#,##", symbols);
        setCommonParams(twoPlacesFormatComma);

        wholeFormat = new DecimalFormat("#", symbols);
        setCommonParams(wholeFormat);
    }

    private static void setCommonParams(DecimalFormat f) {
        f.setRoundingMode(RoundingMode.HALF_UP);
        f.setGroupingSize(3);
        f.setGroupingUsed(true);
    }

    public static Long roundWhole(double value) {
        return new BigDecimal(value)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    public static String formatTwoPlacesDot(double value) {
        return twoPlacesFormatDot.format(value);
    }

    public static String formatTwoPlacesComma(double value) {
        return twoPlacesFormatComma.format(value);
    }

    public static String formatWhole(double value) {
        return wholeFormat.format(value);
    }
}

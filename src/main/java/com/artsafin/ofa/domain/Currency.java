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
        DecimalFormatSymbols dotSpace = new DecimalFormatSymbols();
        dotSpace.setGroupingSeparator(' ');
        dotSpace.setDecimalSeparator('.');

        DecimalFormatSymbols comma = new DecimalFormatSymbols();
        comma.setDecimalSeparator(',');

        twoPlacesFormatDot = new DecimalFormat("#.00", dotSpace);
        twoPlacesFormatDot.getDecimalFormatSymbols().setDecimalSeparator('.');
        setCommonParams(twoPlacesFormatDot);

        twoPlacesFormatComma = new DecimalFormat("#.00", comma);
        twoPlacesFormatComma.setRoundingMode(RoundingMode.HALF_UP);
        twoPlacesFormatComma.setGroupingUsed(false);

        wholeFormat = new DecimalFormat("#", dotSpace);
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

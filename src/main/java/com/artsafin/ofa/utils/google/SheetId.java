package com.artsafin.ofa.utils.google;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.StringJoiner;

public class SheetId {
    public final int sheetId;

    @Nullable
    public String sheetTitle;

    @Nonnull
    public final String spreadsheetId;

    public SheetId(@Nonnull String spreadsheetId, int sheetId, @Nonnull String sheetTitle) {
        this.sheetId = sheetId;
        this.spreadsheetId = spreadsheetId;
        this.sheetTitle = sheetTitle;
    }

    public SheetId(@Nonnull String spreadsheetId, int sheetId) {
        this.sheetId = sheetId;
        this.spreadsheetId = spreadsheetId;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SheetId.class.getSimpleName() + "[", "]")
                .add("sheetId=" + sheetId)
                .add("sheetTitle='" + sheetTitle + "'")
                .add("spreadsheetId='" + spreadsheetId + "'")
                .toString();
    }
}

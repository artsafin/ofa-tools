package com.artsafin.ofa.utils.google;

import com.google.api.services.sheets.v4.model.ValueRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValueRangeBuilder {
    private final List<ValueRange> values = new ArrayList<>();

    private final SheetId sheetId;

    public ValueRangeBuilder(SheetId sheetId) {
        this.sheetId = sheetId;
    }

    public ValueRangeBuilder addSingleValue(String cell, Object cellValue) {
        ValueRange value = new ValueRange()
                .setRange(sheetId.sheetTitle + "!" + cell)
                .setValues(Collections.singletonList(Collections.singletonList(cellValue)));

        values.add(value);

        return this;
    }

    public List<ValueRange> build() {
        return values;
    }
}

package com.artsafin.ofa.utils.google;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.BatchGet;
import com.google.api.services.sheets.v4.model.*;

import java.io.IOException;
import java.util.*;

public class SpreadsheetsService {

    private final GoogleServiceFactory factory;
    private Sheets service;

    public static String getUrl(String spreadsheetId) {
        return String.format("https://docs.google.com/spreadsheets/d/%s/edit", spreadsheetId);
    }

    public static String getUrl(SheetId sheetId) {
        return String.format("https://docs.google.com/spreadsheets/d/%s/edit#gid=%s", sheetId.spreadsheetId, sheetId.sheetId);
    }

    public SpreadsheetsService(GoogleServiceFactory factory) {
        this.factory = factory;
    }

    private Sheets getOrCreateService() throws IOException {
        if (service == null) {
            service = factory.createSheetsService();
        }

        return service;
    }

    private String createNewSheet(SpreadsheetProperties newProperties) throws IOException {
        Sheets service = getOrCreateService();

        Spreadsheet newSpreadsheet = new Spreadsheet();
        newSpreadsheet.setProperties(newProperties);
        newSpreadsheet = service.spreadsheets()
                .create(newSpreadsheet)
                .setFields("spreadsheetId")
                .execute();

        return newSpreadsheet.getSpreadsheetId();
    }

    private void deleteSheetsExcept(String spreadsheetId, int exceptSheetId) throws IOException {
        Sheets service = getOrCreateService();

        Spreadsheet newSpreadsheet = service.spreadsheets().get(spreadsheetId).setFields("sheets").execute();

        if (newSpreadsheet.getSheets().size() > 1) {
            List<Request> requests = new ArrayList<>();
            for (Sheet sheet : newSpreadsheet.getSheets()) {
                if (!sheet.getProperties().getSheetId().equals(exceptSheetId)) {
                    requests.add(new Request().setDeleteSheet(new DeleteSheetRequest().setSheetId(sheet.getProperties().getSheetId())));
                }
            }

            if (requests.size() > 0) {
                BatchUpdateSpreadsheetRequest batchUpdate = new BatchUpdateSpreadsheetRequest();
                batchUpdate.setRequests(requests);
                service.spreadsheets().batchUpdate(spreadsheetId, batchUpdate).execute();
            }
        }
    }

    public SheetId createSheetFrom(SheetId from, SpreadsheetProperties newProperties) throws IOException {
        String newSpreadsheetId = createNewSheet(newProperties);

        CopySheetToAnotherSpreadsheetRequest copyParams = new CopySheetToAnotherSpreadsheetRequest()
                .setDestinationSpreadsheetId(newSpreadsheetId);

        SheetProperties newSheet = service.spreadsheets().sheets().copyTo(from.spreadsheetId, from.sheetId, copyParams).execute();

        deleteSheetsExcept(newSpreadsheetId, newSheet.getSheetId());

        return new SheetId(newSpreadsheetId, newSheet.getSheetId(), newSheet.getTitle());
    }

    public void updateValues(SheetId sheetId, List<ValueRange> values) throws IOException {
        Sheets service = getOrCreateService();

        BatchUpdateValuesRequest updateValuesRequest = new BatchUpdateValuesRequest();
        updateValuesRequest.setValueInputOption("USER_ENTERED");
        updateValuesRequest.setData(values);
        service.spreadsheets().values().batchUpdate(sheetId.spreadsheetId, updateValuesRequest).execute();
    }

    public Map<String, String> readCells(SheetId sheetId, List<String> ranges) throws IOException {
        Sheets service = getOrCreateService();

        BatchGet batchGet = service.spreadsheets().values().batchGet(sheetId.spreadsheetId);
        batchGet.setRanges(ranges);

        List<ValueRange> valueRanges = batchGet.execute().getValueRanges();

        Map<String, String> cells = new HashMap<>();
        for (ValueRange r: valueRanges) {
            String cellValue = "";
            if (r.getValues().size() > 0 && r.getValues().get(0).size() > 0) {
                cellValue = r.getValues().get(0).get(0).toString();
            }
            String cellName = r.getRange();
            cells.put(cellName.substring(cellName.indexOf("!") + 1), cellValue);
        }

        return cells;
    }
}

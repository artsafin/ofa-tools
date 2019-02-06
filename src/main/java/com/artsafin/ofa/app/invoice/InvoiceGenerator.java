package com.artsafin.ofa.app.invoice;

import com.artsafin.ofa.Main;
import com.artsafin.ofa.app.AirtableData;
import com.artsafin.ofa.app.AmbiguousInvoiceException;
import com.artsafin.ofa.app.InvoiceNotFoundException;
import com.artsafin.ofa.domain.Invoice;
import com.artsafin.ofa.utils.AppConfig;
import com.artsafin.ofa.utils.EnglishNumberToWords;
import com.artsafin.ofa.utils.airtable.AirtableLoadException;
import com.artsafin.ofa.utils.google.DriveService;
import com.artsafin.ofa.utils.google.DriveService.Folder;
import com.artsafin.ofa.utils.google.SheetId;
import com.artsafin.ofa.utils.google.SpreadsheetsService;
import com.artsafin.ofa.utils.google.ValueRangeBuilder;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public class InvoiceGenerator {
    private final SheetId templateSheet;
    private final PrintStream out;
    private final AppConfig cfg;
    private final AirtableData airtable;
    private final SpreadsheetsService spreadsheets;
    private final DriveService drive;

    public InvoiceGenerator(PrintStream out, AppConfig cfg, AirtableData airtable, SpreadsheetsService spreadsheets, DriveService drive) {
        this.out = out;
        this.cfg = cfg;
        this.airtable = airtable;
        this.spreadsheets = spreadsheets;
        this.drive = drive;

        templateSheet = new SheetId(cfg.invoiceTemplateFile(), cfg.invoiceTemplateSheet());
    }

    public void generateInvoice(final Main.CreateInvoiceArgs args) throws AirtableLoadException, AmbiguousInvoiceException, InvoiceNotFoundException, IOException {
        out.println("Generating invoice document by ID: " + args.invoiceIdPart);

        Invoice invoice = airtable.findOneInvoice(args.invoiceIdPart);

        out.println("Found invoice: " + invoice.id);

        String filename = invoice.filename + "-ofa";

        SheetId newSheet = createNewInvoiceSheetFromTemplate(invoice, filename);

        List<File> replacedFiles = replaceFile(cfg.invoiceFolder(), filename, newSheet.spreadsheetId);
        if (replacedFiles.size() > 0) {
            Stream<String> deletedFileLabels = replacedFiles.stream().map((file) -> String.format("%s (%s)", file.getName(), SpreadsheetsService.getUrl(file.getId())));
            out.println("Deleted previous version: " + deletedFileLabels.collect(joining(", ")));
        }

        out.printf("Generated %s: %s\n", filename, SpreadsheetsService.getUrl(newSheet));
    }

    private String getTotalAsWords(double total) {
        total = ((int) (100 * total)) / 100.0;

        int wholePart = (int) total;
        String wholePartString = EnglishNumberToWords.convert(wholePart);
        wholePartString = wholePartString.substring(0, 1).toUpperCase() + wholePartString.substring(1);

        String totalStr = String.valueOf((int) (total * 100));
        String cents = totalStr.substring(totalStr.length() - 2);

        return String.format("%s euro and %s cents", wholePartString, cents);
    }

    private List<File> replaceFile(String folderId, String replaceFilename, String fileId) throws IOException {
        Folder folder = drive.new Folder(folderId);

        List<File> replaced = folder.deleteIfExists(replaceFilename);

        folder.moveFileHere(fileId);

        return replaced;
    }

    private SheetId createNewInvoiceSheetFromTemplate(Invoice invoice, String filename) throws IOException {
        Map<String, String> tplValues = spreadsheets.readCells(templateSheet, Arrays.asList("AM5", "AM6", "D28", "Z28", "AE28", "AH28", "AH30", "AM32", "AM33"));

        SheetId newSheet = spreadsheets.createSheetFrom(templateSheet, (new SpreadsheetProperties()).setTitle(filename));

        ValueRangeBuilder values = new ValueRangeBuilder(newSheet);
        values.addSingleValue("AM5", tplValues.get("AM5").replace("%D%", String.valueOf(invoice.number)));
        values.addSingleValue("AM6", tplValues.get("AM6").replace("%MMMM DD YYYY%", invoice.invoiceDateYmd()));
        values.addSingleValue("D28", tplValues.get("D28").replace("%MMMM YYYY%", invoice.invoiceDateYm()));
        values.addSingleValue("Z28", tplValues.get("Z28").replace("%D%", String.valueOf(invoice.hourRate)));
        values.addSingleValue("AE28", tplValues.get("AE28").replace("%D%", String.valueOf(invoice.hours)));
        values.addSingleValue("AH28", tplValues.get("AH28").replace("%D%", String.valueOf(invoice.total)));
        values.addSingleValue("AH30", tplValues.get("AH30").replace("%D%", String.valueOf(invoice.total)));
        values.addSingleValue("AM32", tplValues.get("AM32").replace("%D%", String.valueOf(invoice.total)));
        values.addSingleValue("AM33", tplValues.get("AM33").replace("%SSSS%", getTotalAsWords(invoice.total)));

        spreadsheets.updateValues(newSheet, values.build());

        return newSheet;
    }
}

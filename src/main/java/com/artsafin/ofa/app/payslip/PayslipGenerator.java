package com.artsafin.ofa.app.payslip;

import com.artsafin.ofa.Main;
import com.artsafin.ofa.app.PayslipsLoadException;
import com.artsafin.ofa.domain.*;
import com.artsafin.ofa.utils.AppConfig;
import com.artsafin.ofa.utils.airtable.AirtableApiClient;
import com.artsafin.ofa.utils.airtable.AirtableLoadException;
import com.artsafin.ofa.utils.airtable.AirtableRecord;
import com.artsafin.ofa.utils.airtable.AirtableTable;
import com.artsafin.ofa.utils.google.DriveService;
import com.artsafin.ofa.utils.google.SheetId;
import com.artsafin.ofa.utils.google.SpreadsheetsService;
import com.artsafin.ofa.utils.google.ValueRangeBuilder;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;

import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.stream.Collectors.toList;

public class PayslipGenerator {
    private static final DateTimeFormatter DATE_RU_FULL = ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATE_EN_MONTH = ofPattern("LLLL yyyy", Locale.US);
    private static final DateTimeFormatter DATE_RU_MONTH = ofPattern("LLLL yyyy", new Locale("ru", "RU"));

    private final SheetId templateSheet;
    private final AppConfig cfg;
    private final PrintStream out;
    private final AirtableApiClient airtableApiClient;
    private final SpreadsheetsService spreadsheets;
    private final DriveService drive;

    public PayslipGenerator(PrintStream out, AppConfig cfg, AirtableApiClient airtableApiClient, SpreadsheetsService spreadsheets, DriveService drive) {
        this.out = out;
        this.airtableApiClient = airtableApiClient;
        this.spreadsheets = spreadsheets;
        this.drive = drive;
        this.cfg = cfg;

        templateSheet = new SheetId(cfg.payslipTemplateFile(), cfg.payslipTemplateSheet());
    }

    private Stream<Result<String, Payslip>> loadPayslips(LocalDate date, String employeeName) throws AirtableLoadException {
        String payslipDate = date.format(ofPattern("LLLL yyyy", Locale.US));

        String formula = String.format("FIND('%s', Subject)", payslipDate);
        if (employeeName != null) {
            formula = String.format("AND(%s, FIND('%s', Subject))", formula, employeeName);
        }

        AirtableApiClient.Query salariesQuery = new AirtableApiClient.Query(cfg.airtableAppId())
                .table("Monthly salaries").filterByFormula(formula);

        AirtableTable<MonthlySalary> msTable = new AirtableTable<>(salariesQuery, airtableApiClient);
        MonthlySalaryRecords ms = msTable.getRecords(MonthlySalaryRecords.class);

        AirtableApiClient.Query emplQuery = new AirtableApiClient.Query(cfg.airtableAppId()).table("Employees");
        AirtableTable<Employee> emplTable = new AirtableTable<>(emplQuery, airtableApiClient);

        return ms.records.stream()
                .filter((AirtableRecord<MonthlySalary> salary) -> salary.fields.payslipNo != 0)
                .map((AirtableRecord<MonthlySalary> salary) -> {
                    String personId = salary.fields.person.get(0);
                    EmployeeRecord rec;

                    try {
                        rec = emplTable.getRecord(personId, EmployeeRecord.class);
                    } catch (AirtableLoadException e) {
                        return Result.<String, Payslip>ofError("Could not load employee record: " + personId);
                    }

                    return Result.<String, Payslip>ofResult(new Payslip(rec.fields, salary.fields));
                })
                .filter((Result<String, Payslip> pr) -> pr.map((p) -> p.employee.isEmploymentContract(), (p) -> true));
    }

    private SheetId createPayslipSheet(String title, LocalDate payslipDate, LocalDate paydayDate, Payslip payslip) throws IOException {
        String periodNameRu = payslipDate.format(DATE_RU_MONTH);

        String headerCellTplValue = spreadsheets.readCells(templateSheet, Collections.singletonList("A9")).get("A9");

        SheetId newSheet = spreadsheets.createSheetFrom(templateSheet, new SpreadsheetProperties().setTitle(title));

        String paydayDateInRussian = paydayDate.format(DATE_RU_FULL);

        String period1Name = String.format("%s - %s", payslipDate.withDayOfMonth(1).format(DATE_RU_FULL), payslipDate.withDayOfMonth(15).format(DATE_RU_FULL));
        String period2Name = String.format("%s - %s", payslipDate.withDayOfMonth(16).format(DATE_RU_FULL), payslipDate.withDayOfMonth(1).plusMonths(1).minusDays(1).format(DATE_RU_FULL));

        ValueRangeBuilder values = new ValueRangeBuilder(newSheet);
        values.addSingleValue("C6", payslip.employee.russianName);
        values.addSingleValue("H6", payslip.employee.employeeNo);
        values.addSingleValue("A9", headerCellTplValue.replace("%D%", String.valueOf(payslip.payslipNo)).replace("%dd.MM.yyyy%", paydayDateInRussian));
        values.addSingleValue("C13", periodNameRu);

        int nextRow = 19;
        values.addSingleValue("D" + nextRow, period1Name);
        values.addSingleValue("E" + nextRow, payslip.period1.daysWorked);
        values.addSingleValue("F" + nextRow, payslip.period1.grossSalaryFormatted());

        nextRow++;
        values.addSingleValue("D" + nextRow, period2Name);
        values.addSingleValue("E" + nextRow, payslip.period2.daysWorked);
        values.addSingleValue("F" + nextRow, payslip.period2.grossSalaryFormatted());

        if (payslip.period1.hasBonus()) {
            nextRow++;
            values.addSingleValue("D" + nextRow, period1Name);
            values.addSingleValue("F" + nextRow, payslip.period1.grossBonusFormatted());
        }

        if (payslip.period2.hasBonus()) {
            nextRow++;
            values.addSingleValue("D" + nextRow, period2Name);
            values.addSingleValue("F" + nextRow, payslip.period2.grossBonusFormatted());
        }

        if (nextRow == 20) {
            values.addSingleValue("A21", "");
            values.addSingleValue("A22", "");
        }

        if (nextRow == 21) {
            values.addSingleValue("A22", "");
        }

        values.addSingleValue("F27", payslip.getTaxFormatted());
        values.addSingleValue("D37", payslip.getTraumatismFormatted());
        values.addSingleValue("D38", payslip.getPensionFormatted());
        values.addSingleValue("G44", paydayDateInRussian);

        spreadsheets.updateValues(newSheet, values.build());

        return newSheet;
    }

    public void generatePayslip(final Main.PayslipArgs args) throws AirtableLoadException, PayslipsLoadException {
        LocalDate paydayDate = args.getParsedPaydayDate();
        LocalDate payslipDate = paydayDate.minusMonths(1);
        out.println("Fetching data for " + payslipDate.format(DATE_EN_MONTH) + ((args.employee != null) ? " and employee " + args.employee : ""));

        List<Result<String, Payslip>> payslipsOrErrors = loadPayslips(payslipDate, args.employee).collect(toList());
        List<String> loadErrors = Result.errorsOnly(payslipsOrErrors).collect(toList());
        if (loadErrors.size() > 0) {
            throw new PayslipsLoadException(loadErrors);
        }

        List<Payslip> payslips = Result.resultsOnly(payslipsOrErrors).collect(toList());

        out.println("Loaded " + payslips.size() + " payslips");

        for (Payslip payslip : payslips) {
            String periodNameEn = payslipDate.format(DATE_EN_MONTH);
            String fileName = "Payslip " + payslip.employee.name + " " + periodNameEn;

            out.println(fileName);

            try {
                DriveService.Folder folder = drive.findOneFolder(cfg.payslipFolder(), payslip.employee.name);

                out.printf("  Will be placed under folder: %s (%s)\n", folder.getName().orElse("(unknown)"), folder.getUrl());

                SheetId newSheet = createPayslipSheet(fileName, payslipDate, paydayDate, payslip);

                List<File> deletedFiles = folder.deleteIfExists(fileName);

                for (File deletedFile : deletedFiles) {
                    out.printf("  Deleted previous payslip: %s (%s)\n", deletedFile.getName(), SpreadsheetsService.getUrl(deletedFile.getId()));
                }

                folder.moveFileHere(newSheet.spreadsheetId);

                // TODO download in better condition
                //drive.downloadAsPdf(newSheet.spreadsheetId, title.replace(' ', '_') + ".pdf");

                out.printf("  Created payslip for %s: %s\n", payslip.employee.name, SpreadsheetsService.getUrl(newSheet));
            } catch (DriveService.FindException | IOException e) {
                out.printf("Skipping payslip %s: %s\n", payslip, e.getMessage());
            }
        }
    }
}

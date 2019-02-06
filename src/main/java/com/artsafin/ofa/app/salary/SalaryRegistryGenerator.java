package com.artsafin.ofa.app.salary;

import com.artsafin.ofa.Main;
import com.artsafin.ofa.app.AirtableData;
import com.artsafin.ofa.app.PayslipsLoadException;
import com.artsafin.ofa.domain.Payslip;
import com.artsafin.ofa.domain.Result;
import com.artsafin.ofa.utils.AppConfig;
import com.artsafin.ofa.utils.airtable.AirtableLoadException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.stream.Collectors.toList;

public class SalaryRegistryGenerator {
    private final PrintStream out;
    private final AppConfig cfg;
    private final AirtableData airtable;

    public SalaryRegistryGenerator(PrintStream out, AppConfig cfg, AirtableData airtable) {

        this.out = out;
        this.cfg = cfg;
        this.airtable = airtable;
    }

    public void generateSalaryRegistry(Main.SalaryRegistryArgs args) throws AirtableLoadException, PayslipsLoadException, IOException {
        List<Result<String, Payslip>> payslipsOrErrors = airtable.createPayslips(args.getParsedDate(), null).collect(toList());
        List<String> loadErrors = Result.errorsOnly(payslipsOrErrors).collect(toList());
        if (loadErrors.size() > 0) {
            throw new PayslipsLoadException(loadErrors);
        }

        List<Payslip> payslips = Result.resultsOnly(payslipsOrErrors).collect(toList());

        String filename = String.format("%s/%s-period%d.xlsx", cfg.docDir(), args.getParsedDate().format(ofPattern("YYYY-MM")), args.period);

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Sheet0");

        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Номер");
        headerRow.createCell(1).setCellValue("Фамилия");
        headerRow.createCell(2).setCellValue("Имя");
        headerRow.createCell(3).setCellValue("Отчество");
        headerRow.createCell(4).setCellValue("Номер счета/Номер договора");
        headerRow.createCell(5).setCellValue("Сумма");

        for (int i = 0; i < payslips.size(); i++) {
            Row row = sheet.createRow(i + 1);

            Payslip payslip = payslips.get(i);
            String[] lastFirstFather = payslip.employee.russianNameLastFirstFather();

            if (lastFirstFather == null) {
                out.printf("Skip employee %s: russian name cannot be parsed\n", payslip.employee.name);
                continue;
            }

            double amount = (args.period == 1) ? payslip.period1.netSalary() : payslip.period2.netSalary();

            row.createCell(0).setCellValue(i + 1);
            row.createCell(1).setCellValue(lastFirstFather[0]);
            row.createCell(2).setCellValue(lastFirstFather[1]);
            row.createCell(3).setCellValue(lastFirstFather[2]);
            row.createCell(4).setCellValue(payslip.employee.accountNo);
            row.createCell(5).setCellValue(amount);
        }

        try (OutputStream fileOut = new FileOutputStream(filename)) {
            wb.write(fileOut);
        }
    }
}

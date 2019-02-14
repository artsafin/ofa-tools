package com.artsafin.ofa;

import com.artsafin.ofa.app.AirtableData;
import com.artsafin.ofa.app.AmbiguousInvoiceException;
import com.artsafin.ofa.app.PayslipsLoadException;
import com.artsafin.ofa.app.approvalrequest.ApprovalRequestGenerator;
import com.artsafin.ofa.app.checker.CheckerService;
import com.artsafin.ofa.app.invoice.InvoiceGenerator;
import com.artsafin.ofa.app.InvoiceNotFoundException;
import com.artsafin.ofa.app.payslip.PayslipGenerator;
import com.artsafin.ofa.app.salary.SalaryRegistryGenerator;
import com.artsafin.ofa.utils.AppConfig;
import com.artsafin.ofa.utils.airtable.*;
import com.artsafin.ofa.utils.google.*;
import com.artsafin.ofa.utils.redmine.RedmineSpentTimeReport;
import com.beust.jcommander.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class Main {
    private static final String APPLICATION_NAME = "Invoice Generator";
    private static final AppConfig cfg = new AppConfig();
    private static AirtableData airtable;
    private static SpreadsheetsService spreadsheets;
    private static DriveService drive;
    private static RedmineSpentTimeReport spentTimeReport;
    private static JCommander jcargs;

    private static MainArgs mainArgs = new MainArgs();

    @Parameters()
    private static class MainArgs {

        @Parameter(names = {"--help", "-h"}, description = "Show help", help = true)
        public boolean help;
    }

    @Parameters(commandDescription = "Generate HTML report describing invoice and detalizing work done for approval", separators = "=")
    public static class ApprovalRequestArgs {

        @Parameter(names = "--invoice", required = true, description = "Part of invoice ID to look for. Invoice ID includes invoice number and short month name. Provided INVOICE-ID must matche only one invoice.")
        public String invoiceIdPart;
    }

    @Parameters(commandDescription = "Generate invoice document for signing (in Google Drive)", separators = "=")
    public static class CreateInvoiceArgs {

        @Parameter(names = "--invoice", required = true, description = "Part of invoice ID to look for. Invoice ID includes invoice number and short month name. Provided INVOICE-ID must matche only one invoice.")
        public String invoiceIdPart;
    }

    @Parameters(commandDescription = "Generate payslip document (in Google Drive)", separators = "=")
    public static class PayslipArgs {

        @Parameter(names = "--payday-date", required = true, description = "Date when the salary is fully paid for previous month")
        public String paydayDate;

        @Parameter(names = "--employee", description = "Only generate for employee with specified name")
        public String employee;

        public LocalDate getParsedPaydayDate() {
            return LocalDate.parse(paydayDate);
        }
    }

    @Parameters(commandDescription = "Generate salary registry to upload to bank", separators = "=")
    public static class SalaryRegistryArgs {
        @Parameter(names = "--date", required = true, description = "Date of month for which salary should be paid")
        public String date;

        @Parameter(names = "--period", required = true, description = "Period to generate registry for. Either 1 or 2.")
        public int period;

        public LocalDate getParsedDate() {
            return LocalDate.parse(date);
        }
    }

    @Parameters(commandDescription = "Check validity of the accounting data", separators = "=")
    public static class CheckerArgs {
        @Parameter(names = "--employee", description = "Only check for employee with specified name")
        public String employee;
    }

    private static final Map<String, Object> subcommands = new HashMap<>();

    static {
        try {
            cfg.load(Main.class.getClassLoader().getResourceAsStream("config.properties"));
            airtable = new AirtableData(cfg, new AirtableApiClient(cfg.airtableToken()));
            spentTimeReport = new RedmineSpentTimeReport(cfg.redmineUrl(), cfg.redmineKey());

            GoogleServiceFactory googleServiceFactory = new GoogleServiceFactory(APPLICATION_NAME);
            spreadsheets = new SpreadsheetsService(googleServiceFactory);
            drive = new DriveService(googleServiceFactory);

            subcommands.put("approvalreq", new ApprovalRequestArgs());
            subcommands.put("invoice", new CreateInvoiceArgs());
            subcommands.put("payslip", new PayslipArgs());
            subcommands.put("salary", new SalaryRegistryArgs());
            subcommands.put("check", new CheckerArgs());

            JCommander.Builder builder = JCommander.newBuilder();
            subcommands.forEach((key, value) -> builder.addCommand(key, value));
            jcargs = builder.addObject(mainArgs).build();

        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    private static void printUsageAndExit(String prefix, int exitCode) {
        StringBuilder sb = new StringBuilder();
        jcargs.usage(sb);

        System.out.println(((prefix != null) ? "Error: " + prefix + "\n\n" : "") + sb.toString());

        System.exit(exitCode);
    }

    public static void main(String... rawArgs) {
        try {
            jcargs.parse(rawArgs);
        } catch (ParameterException e) {
            printUsageAndExit(e.getMessage(), 1);
        }

        if (mainArgs.help) {
            printUsageAndExit(null, 0);
        }
        if (jcargs.getParsedCommand() == null) {
            printUsageAndExit("No command provided", 1);
        }

        Object args = subcommands.get(jcargs.getParsedCommand());

        if (args == null) {
            printUsageAndExit("Invalid command provided", 1);
        }

        try {
            if (args instanceof CreateInvoiceArgs) {
                InvoiceGenerator app = new InvoiceGenerator(
                        System.out,
                        cfg,
                        airtable,
                        spreadsheets,
                        drive
                );
                app.generateInvoice((CreateInvoiceArgs) args);
            }
            if (args instanceof ApprovalRequestArgs) {
                ApprovalRequestGenerator app = new ApprovalRequestGenerator(
                        System.out,
                        cfg,
                        airtable,
                        spentTimeReport
                );
                app.generateApprovalRequest((ApprovalRequestArgs) args);
            }
            if (args instanceof PayslipArgs) {
                PayslipGenerator app = new PayslipGenerator(
                        System.out,
                        cfg,
                        airtable,
                        spreadsheets,
                        drive
                );
                app.generatePayslip((PayslipArgs) args);
            }
            if (args instanceof SalaryRegistryArgs) {
                SalaryRegistryGenerator app = new SalaryRegistryGenerator(
                        System.out,
                        cfg,
                        airtable
                );
                app.generateSalaryRegistry((SalaryRegistryArgs) args);
            }
            if (args instanceof CheckerArgs) {
                CheckerService app = new CheckerService(
                        System.out,
                        airtable,
                        cfg
                );
                app.check((CheckerArgs) args);
            }
        } catch (DateTimeParseException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (AmbiguousInvoiceException e) {
            System.err.println("Ambiguous invoice found for ID " + e.invoiceIdPart + ": " + e.ids);
            System.exit(1);
        } catch (InvoiceNotFoundException e) {
            System.err.println("Invoice with specified ID not found: " + e.invoiceIdPart);
            System.exit(1);
        } catch (PayslipsLoadException e) {
            System.err.println("Error loading payslips: " + e.getErrors("\n"));
            System.exit(1);
        } catch (IOException | AirtableLoadException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
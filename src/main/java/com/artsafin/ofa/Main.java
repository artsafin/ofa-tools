package com.artsafin.ofa;

import com.artsafin.ofa.app.AmbiguousInvoiceException;
import com.artsafin.ofa.app.PayslipsLoadException;
import com.artsafin.ofa.app.approvalrequest.ApprovalRequestGenerator;
import com.artsafin.ofa.app.invoice.InvoiceGenerator;
import com.artsafin.ofa.app.InvoiceNotFoundException;
import com.artsafin.ofa.app.payslip.PayslipGenerator;
import com.artsafin.ofa.utils.AppConfig;
import com.artsafin.ofa.utils.airtable.*;
import com.artsafin.ofa.utils.google.*;
import com.artsafin.ofa.utils.redmine.RedmineSpentTimeReport;
import com.beust.jcommander.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class Main {
    private static final String APPLICATION_NAME = "Invoice Generator";
    private static final AppConfig cfg = new AppConfig();
    private static AirtableApiClient airtableApiClient;
    private static SpreadsheetsService spreadsheets;
    private static DriveService drive;
    private static RedmineSpentTimeReport spentTimeReport;
    private static JCommander jcargs;

    private static MainArgs jcCmdMain = new MainArgs();
    private static ApprovalRequestArgs jcCmdApprovalRequest = new ApprovalRequestArgs();
    private static CreateInvoiceArgs jcCmdCreateInvoice = new CreateInvoiceArgs();
    private static PayslipArgs jcCmdPayslip = new PayslipArgs();

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

    private static final String CMD_APPROVALREQ = "approvalreq";
    private static final String CMD_INVOICE = "invoice";
    private static final String CMD_PAYSLIP = "payslip";

    static {
        try {
            cfg.load(Main.class.getClassLoader().getResourceAsStream("config.properties"));
            airtableApiClient = new AirtableApiClient(cfg.airtableToken());
            spentTimeReport = new RedmineSpentTimeReport(cfg.redmineUrl(), cfg.redmineKey());

            GoogleServiceFactory googleServiceFactory = new GoogleServiceFactory(APPLICATION_NAME);
            spreadsheets = new SpreadsheetsService(googleServiceFactory);
            drive = new DriveService(googleServiceFactory);

            jcargs = JCommander.newBuilder()
                    .addObject(jcCmdMain)
                    .addCommand(CMD_APPROVALREQ, jcCmdApprovalRequest)
                    .addCommand(CMD_INVOICE, jcCmdCreateInvoice)
                    .addCommand(CMD_PAYSLIP, jcCmdPayslip)
                    .build();
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

        if (jcCmdMain.help) {
            printUsageAndExit(null, 0);
        }
        if (jcargs.getParsedCommand() == null) {
            printUsageAndExit("No command provided", 1);
        }

        try {
            if (jcargs.getParsedCommand().equals(CMD_INVOICE)) {
                InvoiceGenerator app = new InvoiceGenerator(
                        System.out,
                        cfg,
                        airtableApiClient,
                        spreadsheets,
                        drive
                );
                app.generateInvoice(jcCmdCreateInvoice);
            }
            if (jcargs.getParsedCommand().equals(CMD_APPROVALREQ)) {
                ApprovalRequestGenerator app = new ApprovalRequestGenerator(
                        System.out,
                        cfg,
                        airtableApiClient,
                        spentTimeReport
                );
                app.generateApprovalRequest(jcCmdApprovalRequest);
            }
            if (jcargs.getParsedCommand().equals(CMD_PAYSLIP)) {
                PayslipGenerator app = new PayslipGenerator(
                        System.out,
                        cfg,
                        airtableApiClient,
                        spreadsheets,
                        drive
                );
                app.generatePayslip(jcCmdPayslip);
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
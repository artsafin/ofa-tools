package com.artsafin.ofa.app.approvalrequest;

import com.artsafin.ofa.Main;
import com.artsafin.ofa.app.AmbiguousInvoiceException;
import com.artsafin.ofa.app.InvoiceNotFoundException;
import com.artsafin.ofa.domain.*;
import com.artsafin.ofa.utils.AppConfig;
import com.artsafin.ofa.utils.airtable.AirtableApiClient;
import com.artsafin.ofa.utils.airtable.AirtableLoadException;
import com.artsafin.ofa.utils.airtable.AirtableTable;
import com.artsafin.ofa.utils.redmine.RedmineSpentTimeReport;
import com.artsafin.ofa.utils.redmine.RedmineSpentTimeReport.ReportParams;
import com.artsafin.ofa.utils.redmine.SpentTimeEntry;
import de.neuland.jade4j.JadeConfiguration;
import de.neuland.jade4j.template.ClasspathTemplateLoader;
import de.neuland.jade4j.template.JadeTemplate;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

public class ApprovalRequestGenerator {
    private final RedmineSpentTimeReport spentTimeReport;
    private final PrintStream out;
    private final AppConfig cfg;
    private final AirtableApiClient airtableApiClient;

    public ApprovalRequestGenerator(PrintStream out, AppConfig cfg, AirtableApiClient airtableApiClient, RedmineSpentTimeReport spentTimeReport) {
        this.out = out;
        this.cfg = cfg;
        this.airtableApiClient = airtableApiClient;
        this.spentTimeReport = spentTimeReport;
    }

    public void generateApprovalRequest(final Main.ApprovalRequestArgs args) throws AirtableLoadException, AmbiguousInvoiceException, InvoiceNotFoundException, IOException {
        out.println("Generating approval request post by invoice ID: " + args.invoiceIdPart);

        Invoice invoice = findOneInvoice(args.invoiceIdPart);

        ExpensesRecords expenses = findExpensesByInvoice(invoice.id);

        out.println("Found " + expenses.records.size() + " expenses");

        List<ExpenseGroup> expensesByTag = expenses.records
                .stream().collect(groupingBy((r) -> r.fields.tag, mapping((r) -> r.fields, toList())))
                .entrySet().stream().map((e) -> new ExpenseGroup(e.getKey(), e.getValue())).collect(Collectors.toList());

        out.printf("Generating timesheet from %s to %s\n", invoice.invoiceDateBeginIso(), invoice.invoiceDateIso());

        ReportParams timesheetParams = new ReportParams(cfg.redmineReportUserIds(), invoice.invoiceDateBeginIso(), invoice.invoiceDateIso());
        List<SpentTimeEntry> spentTimes = spentTimeReport.loadEntries(timesheetParams);

        List<SpentTimeGroup> spentTimesByUser = spentTimes.stream()
                .sorted((o1, o2) -> o1.issueTitle.compareToIgnoreCase(o2.issueTitle))
                .collect(groupingBy(SpentTimeEntry::getUserName))
                .entrySet().stream()
                .filter((entry) -> entry.getValue().size() > 0)
                .map((entry) -> new SpentTimeGroup(entry.getKey(), entry.getValue()))
                .collect(toList());

        Map<String, Object> tplParams = new TemplateParamBuilder(invoice, expensesByTag, spentTimesByUser, timesheetParams).toMap();
        tplParams.put("redmine_host", spentTimeReport.getHost());

        JadeConfiguration config = new JadeConfiguration();
        config.setTemplateLoader(new ClasspathTemplateLoader());
        config.setPrettyPrint(true);
        JadeTemplate template = config.getTemplate("post.jade");
        try (FileWriter htmlWriter = new FileWriter(invoice.filename + "-post.html")) {
            config.renderTemplate(template, tplParams, htmlWriter);
        }

        out.println("Generated " + invoice.filename + "-post.html");
    }

    private Invoice findOneInvoice(String invoiceIdPart) throws AirtableLoadException, AmbiguousInvoiceException, InvoiceNotFoundException {
        AirtableApiClient.Query searchByIdQuery = new AirtableApiClient.Query(cfg.airtableAppId())
                .table("Invoices").filterByFormula(String.format("FIND('%s', No)", invoiceIdPart));

        AirtableTable<Invoice> invoicesTable = new AirtableTable<>(searchByIdQuery, airtableApiClient);
        InvoicesRecords invoices = invoicesTable.getRecords(InvoicesRecords.class);

        if (invoices.records.size() > 1) {
            String ids = invoices.records.stream().map((r) -> r.fields.id).collect(Collectors.joining(", "));
            throw new AmbiguousInvoiceException(invoiceIdPart, ids);
        }
        if (invoices.records.size() == 0) {
            throw new InvoiceNotFoundException(invoiceIdPart);
        }

        return invoices.records.get(0).fields;
    }

    private ExpensesRecords findExpensesByInvoice(String invoiceId) throws AirtableLoadException {
        AirtableApiClient.Query fetchByIdQuery = new AirtableApiClient.Query(cfg.airtableAppId())
                .table("Expenses").filterByFormula(String.format("{Invoice} = '%s'", invoiceId));

        AirtableTable<Expense> expensesTable = new AirtableTable<>(fetchByIdQuery, airtableApiClient);
        return expensesTable.getRecords(ExpensesRecords.class);
    }
}

package com.artsafin.ofa.app;

import com.artsafin.ofa.domain.*;
import com.artsafin.ofa.utils.AppConfig;
import com.artsafin.ofa.utils.airtable.AirtableApiClient;
import com.artsafin.ofa.utils.airtable.AirtableLoadException;
import com.artsafin.ofa.utils.airtable.AirtableRecord;
import com.artsafin.ofa.utils.airtable.AirtableTable;

import java.time.LocalDate;
import java.util.Locale;
import java.util.stream.Stream;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.stream.Collectors.joining;

public class AirtableData {
    private final AirtableApiClient airtableApiClient;
    private final AppConfig cfg;

    public AirtableData(AppConfig cfg, AirtableApiClient airtableApiClient) {
        this.cfg = cfg;
        this.airtableApiClient = airtableApiClient;
    }

    public Stream<Employee> findActiveEmployees(String employeeName) throws AirtableLoadException {
        AirtableApiClient.Query query = new AirtableApiClient.Query(cfg.airtableAppId()).table("Employees");

        if (employeeName != null) {
            query = query.filterByFormula(String.format("FIND('%s', {Name})", employeeName));
        } else {
            query = query.filterByFormula("{End date} = \"\"");
        }

        AirtableTable<Employee> table = new AirtableTable<>(query, airtableApiClient);
        EmployeeRecords records = table.getRecords(EmployeeRecords.class);

        return records.records.stream().map((atr) -> atr.fields);
    }

    public Stream<Result<String, Payslip>> createPayslips(LocalDate date, String employeeName) throws AirtableLoadException {
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

    public Invoice findOneInvoice(String invoiceIdPart) throws AirtableLoadException, AmbiguousInvoiceException, InvoiceNotFoundException {
        AirtableApiClient.Query searchByIdQuery = new AirtableApiClient.Query(cfg.airtableAppId())
                .table("Invoices").filterByFormula(String.format("FIND('%s', No)", invoiceIdPart));

        AirtableTable<Invoice> invoicesTable = new AirtableTable<>(searchByIdQuery, airtableApiClient);
        InvoicesRecords invoices = invoicesTable.getRecords(InvoicesRecords.class);

        if (invoices.records.size() > 1) {
            String ids = invoices.records.stream().map((r) -> r.fields.id).collect(joining(", "));
            throw new AmbiguousInvoiceException(invoiceIdPart, ids);
        }
        if (invoices.records.size() == 0) {
            throw new InvoiceNotFoundException(invoiceIdPart);
        }

        return invoices.records.get(0).fields;
    }

    public ExpensesRecords findExpensesByInvoice(String invoiceId) throws AirtableLoadException {
        AirtableApiClient.Query fetchByIdQuery = new AirtableApiClient.Query(cfg.airtableAppId())
                .table("Expenses").filterByFormula(String.format("{Invoice} = '%s'", invoiceId));

        AirtableTable<Expense> expensesTable = new AirtableTable<>(fetchByIdQuery, airtableApiClient);
        return expensesTable.getRecords(ExpensesRecords.class);
    }
}

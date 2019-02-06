package com.artsafin.ofa.app.approvalrequest;

import com.artsafin.ofa.domain.Currency;
import com.artsafin.ofa.domain.Invoice;
import com.artsafin.ofa.utils.redmine.RedmineSpentTimeReport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplateParamBuilder {
    public static class Expenses {
        public final List<ExpenseGroup> byUser;
        public final String subtotalRub;
        public final String subtotalEur;
        public final String wholeRounding;
        public final String returnOfRounding;
        public final String totalEur;

        public Expenses(Invoice invoice, List<ExpenseGroup> byUser) {
            this.byUser = byUser;
            this.wholeRounding = Currency.formatTwoPlacesDot(invoice.rounding);
            this.returnOfRounding = Currency.formatTwoPlacesDot(invoice.returnOfRounding);

            subtotalRub = Currency.formatWhole(byUser.stream().mapToDouble((eg) -> eg.totalRub).sum());
            double totalExpensesEurDouble = byUser.stream().mapToDouble((eg) -> eg.totalEur).sum();
            subtotalEur = Currency.formatWhole(totalExpensesEurDouble);
            totalEur = Currency.formatWhole(Currency.roundWhole(totalExpensesEurDouble) + invoice.rounding - invoice.returnOfRounding);
        }
    }

    public static class Timesheet {
        public final List<SpentTimeGroup> byUser;
        public final String totalHours;
        public final RedmineSpentTimeReport.ReportParams params;

        public Timesheet(List<SpentTimeGroup> byUser, RedmineSpentTimeReport.ReportParams params) {
            this.byUser = byUser;
            this.params = params;
            totalHours = Currency.formatTwoPlacesDot(byUser.stream().mapToDouble((st) -> st.totalSpent).sum());
        }
    }

    private final Invoice invoice;
    private final Expenses expenses;
    private final Timesheet timesheet;

    TemplateParamBuilder(Invoice invoice, List<ExpenseGroup> expenseGroups, List<SpentTimeGroup> timesheetByUser, RedmineSpentTimeReport.ReportParams timesheetParams) {
        this.invoice = invoice;

        expenses = new Expenses(invoice, expenseGroups);
        timesheet = new Timesheet(timesheetByUser, timesheetParams);
    }

    Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put("invoice", invoice);
        map.put("expenses", expenses);
        map.put("timesheet", timesheet);

        return map;
    }
}

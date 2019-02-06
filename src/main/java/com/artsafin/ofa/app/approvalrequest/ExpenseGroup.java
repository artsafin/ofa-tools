package com.artsafin.ofa.app.approvalrequest;

import com.artsafin.ofa.domain.Expense;
import com.artsafin.ofa.domain.Currency;

import java.util.List;
import java.util.StringJoiner;

public class ExpenseGroup {
    public final String groupName;
    public final List<Expense> expenses;
    public final double totalRub;
    public final double totalEur;

    ExpenseGroup(String groupName, List<Expense> expenses) {
        this.groupName = groupName;
        this.expenses = expenses;

        totalRub = expenses.stream().mapToDouble((r) -> r.rubAmount).sum();
        totalEur = expenses.stream().mapToDouble((r) -> r.eurAmount).sum();
    }

    public String totalRubRounded() {
        return Currency.formatTwoPlacesDot(totalRub);
    }

    public String totalEurRounded() {
        return Currency.formatTwoPlacesDot(totalEur);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ExpenseGroup.class.getSimpleName() + "[", "]")
                .add("groupName='" + groupName + "'")
                .add("expenses=" + expenses)
                .add("subtotalRub=" + totalRubRounded())
                .add("totalEur=" + totalEurRounded())
                .toString();
    }
}

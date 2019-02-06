package com.artsafin.ofa.domain;

import java.util.StringJoiner;

public class Payslip {
    public static class PayPeriod {
        public final int daysWorked;
        private final long grossSalary;
        private final long grossBonus;
        private final long netSalary;

        public PayPeriod(int daysWorked, long grossSalary, long grossBonus, long netSalary) {
            this.daysWorked = daysWorked;
            this.grossSalary = grossSalary;
            this.grossBonus = grossBonus;
            this.netSalary = netSalary;
        }

        public boolean hasBonus() {
            return grossBonus > 0;
        }

        public String grossSalaryFormatted() {
            return Currency.formatTwoPlacesComma((grossSalary - grossBonus) / 100.0);
        }

        public String netSalaryFormatted() {
            return Currency.formatTwoPlacesComma(netSalary / 100.0);
        }

        public double netSalary() {
            return netSalary / 100.0;
        }

        public String grossBonusFormatted() {
            return Currency.formatTwoPlacesComma(grossBonus / 100.0);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", PayPeriod.class.getSimpleName() + "[", "]")
                    .add("daysWorked=" + daysWorked)
                    .add("grossSalary=" + grossSalary)
                    .add("grossBonus=" + grossBonus)
                    .add("netSalary=" + netSalary)
                    .toString();
        }
    }

    public final Employee employee;
    public final int payslipNo;
    public final PayPeriod period1;
    public final PayPeriod period2;
    private final long tax;
    private final long traumatism;
    private final long pension;

    public Payslip(Employee employee, MonthlySalary salary) {
        this.employee = employee;
        payslipNo = salary.payslipNo;
        period1 = new PayPeriod(salary.daysWorked1, salary.getGrossSalary1(), salary.getGrossBonus1(), salary.getNetSalary1());
        period2 = new PayPeriod(salary.daysWorked2, salary.getGrossSalary2(), salary.getGrossBonus2(), salary.getNetSalary2());
        tax = salary.getTax();
        traumatism = salary.getTraumatism();
        pension = salary.getPension();
    }

    public String getTaxFormatted() {
        return Currency.formatTwoPlacesComma(tax / 100.0);
    }

    public String getTraumatismFormatted() {
        return Currency.formatTwoPlacesComma(traumatism / 100.0);
    }

    public String getPensionFormatted() {
        return Currency.formatTwoPlacesComma(pension / 100.0);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Payslip.class.getSimpleName() + "[", "]")
                .add("employee=" + employee)
                .add("payslipNo=" + payslipNo)
                .add("period1=" + period1)
                .add("period2=" + period2)
                .add("tax=" + tax)
                .add("traumatism=" + traumatism)
                .add("pension=" + pension)
                .toString();
    }
}

package com.artsafin.ofa.domain;

import com.google.api.client.util.Key;

import java.util.List;

public class MonthlySalary {
    @Key("Person")
    public List<String> person;

    @Key("Days worked 1/2M")
    public int daysWorked1;

    @Key("Days worked 2/2M")
    public int daysWorked2;

    @Key("Gross salary 1/2M")
    private double grossSalary1;

    @Key("Gross salary 2/2M")
    private double grossSalary2;

    @Key("Gross bonus 1/2M")
    private double grossBonus1;

    @Key("Gross bonus 2/2M")
    private double grossBonus2;

    @Key("Funds 1M: traumatism")
    private double traumatism;

    @Key("Funds 1M: Pension")
    private double pension;

    @Key("Payslip Ref")
    public int payslipNo;

    @Key("Tax 1M")
    private double tax;

    public long getGrossSalary1() {
        return Math.round(grossSalary1 * 100);
    }

    public long getGrossSalary2() {
        return Math.round(grossSalary2 * 100);
    }

    public long getGrossBonus1() {
        return Math.round(grossBonus1 * 100);
    }

    public long getGrossBonus2() {
        return Math.round(grossBonus2 * 100);
    }

    public long getTraumatism() {
        return Math.round(traumatism * 100);
    }

    public long getPension() {
        return Math.round(pension * 100);
    }

    public long getTax() {
        return Math.round(tax * 100);
    }
}

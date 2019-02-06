package com.artsafin.ofa.domain;

import com.google.api.client.util.Key;

public class Employee {
    @Key("Name")
    public String name;

    @Key("Russian full name")
    public String russianName;

    @Key("No")
    public int employeeNo;

    @Key("Contract")
    private String contract;

    public boolean isEmploymentContract() {
        return contract.equals("td");
    }

    @Override
    public String toString() {
        return "Employee{" +
                "name='" + name + '\'' +
                ", russianName='" + russianName + '\'' +
                ", employeeNo=" + employeeNo +
                ", contract='" + contract + '\'' +
                '}';
    }
}

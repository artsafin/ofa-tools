package com.artsafin.ofa.domain;

import com.google.api.client.util.Key;

import java.util.StringJoiner;

public class Employee {
    @Key("Name")
    public String name;

    @Key("Russian full name")
    public String russianName;

    @Key("No")
    public int employeeNo;

    @Key("Contract")
    private String contract;

    @Key("Tinkoff Account No")
    public String accountNo;

    public boolean isEmploymentContract() {
        return contract.equals("td");
    }

    public String[] russianNameLastFirstFather() {
        String[] s = russianName.split(" ", 3);

        if (s.length != 3) {
            return null;
        }

        return s;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Employee.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("russianName='" + russianName + "'")
                .add("employeeNo=" + employeeNo)
                .add("contract='" + contract + "'")
                .add("accountNo='" + accountNo + "'")
                .toString();
    }
}

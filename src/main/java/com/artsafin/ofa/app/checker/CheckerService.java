package com.artsafin.ofa.app.checker;

import com.artsafin.ofa.Main;
import com.artsafin.ofa.app.AirtableData;
import com.artsafin.ofa.domain.Employee;
import com.artsafin.ofa.utils.AppConfig;
import com.artsafin.ofa.utils.airtable.AirtableLoadException;

import java.io.PrintStream;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;

public class CheckerService {
    private final PrintStream out;
    private final AirtableData airtable;
    private final AppConfig cfg;

    public CheckerService(PrintStream out, AirtableData airtable, AppConfig cfg) {
        this.out = out;
        this.airtable = airtable;
        this.cfg = cfg;
    }

    public void check(Main.CheckerArgs args) throws AirtableLoadException {
        Stream<Employee> empls = airtable.findActiveEmployees(args.employee);

        Map<String, List<String>> employeesByProblem = empls
                .filter(Employee::isEmploymentContract)
                .map((e) -> {
                    List<SimpleEntry<String, String>> problemList = new ArrayList<>();

                    if (e.isRussianNameEmpty()) {
                        problemList.add(new SimpleEntry<>(e.name, "empty russian name"));
                    }

                    if (e.isAccountNoEmpty()) {
                        problemList.add(new SimpleEntry<>(e.name, "empty account number"));
                    }

                    if (e.employeeNo == 0) {
                        problemList.add(new SimpleEntry<>(e.name, "empty employee number"));
                    }

                    if (!e.jobAgreementSigned) {
                        problemList.add(new SimpleEntry<>(e.name, "job agreement not signed"));
                    }

                    if (!e.ndaSigned) {
                        problemList.add(new SimpleEntry<>(e.name, "NDA not signed"));
                    }

                    if (!e.sentToRegina) {
                        problemList.add(new SimpleEntry<>(e.name, "not sent to Regina"));
                    }

                    return problemList;
                })
                .flatMap(Collection::stream)
                .collect(groupingBy(SimpleEntry::getValue, Collectors.mapping(SimpleEntry::getKey, Collectors.toList())));

        for (Entry<String,List<String>> e: employeesByProblem.entrySet()) {
            out.println(e.getKey() + ":");

            for (String name: e.getValue()) {
                out.println("    " + name);
            }
        }
    }
}

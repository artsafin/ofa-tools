package com.artsafin.ofa.app.approvalrequest;

import com.artsafin.ofa.utils.redmine.SpentTimeEntry;

import java.util.List;

public class SpentTimeGroup {
    public final String groupName;
    public final double totalSpent;
    public final List<SpentTimeEntry> entries;

    public SpentTimeGroup(String groupName, List<SpentTimeEntry> entries) {
        this.groupName = groupName;
        this.entries = entries;

        this.totalSpent = entries.stream().mapToDouble(SpentTimeEntry::getHours).sum();
    }
}

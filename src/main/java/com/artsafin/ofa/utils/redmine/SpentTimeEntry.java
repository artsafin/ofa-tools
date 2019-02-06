package com.artsafin.ofa.utils.redmine;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.StringJoiner;

@JsonPropertyOrder({"projectName", "spentOn", "userName", "activityName", "issueTitle", "comment", "hours"})
public class SpentTimeEntry {
    @JsonProperty()
    public String projectName;

    @JsonProperty()
    public String spentOn;

    @JsonProperty()
    public String userName;

    @JsonProperty()
    public String activityName;

    @JsonProperty()
    public String issueTitle;

    @JsonProperty()
    public String comment;

    @JsonProperty()
    public double hours;

    @JsonIgnore
    public String getUserName() {
        return userName;
    }

    @JsonIgnore
    public String getIssueTitle() {
        return issueTitle;
    }

    @JsonIgnore
    public double getHours() {
        return hours;
    }

    @JsonIgnore
    public String issueId() {
        return issueTitle.substring(issueTitle.indexOf('#') + 1, issueTitle.indexOf(':'));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SpentTimeEntry.class.getSimpleName() + "[", "]")
                .add("projectName='" + projectName + "'")
                .add("spentOn='" + spentOn + "'")
                .add("userName='" + userName + "'")
                .add("issueTitle='" + issueTitle + "'")
                .add("comment='" + comment + "'")
//                .add("hours=" + hours)
                .toString();
    }
}

package com.nigealm.gadgets.svc;

/**
 * Created by user on 07/01/2016.
 */
public class SprintTrendDayRecord {
    private int numberOfCommits;
    private int numberOfClosedIssues;
    private int numberOfOpenIssues;
    private int numberOfSuccessfulBuilds;
    private int numberOfFailedBuilds;

    public int getNumberOfCommits() {
        return numberOfCommits;
    }

    public void setNumberOfCommits(int numberOfCommits) {
        this.numberOfCommits = numberOfCommits;
    }

    public int getNumberOfClosedIssues() {
        return numberOfClosedIssues;
    }

    public void setNumberOfClosedIssues(int numberOfClosedIssues) {
        this.numberOfClosedIssues = numberOfClosedIssues;
    }

    public int getNumberOfOpenIssues() {
        return numberOfOpenIssues;
    }

    public void setNumberOfOpenIssues(int numberOfOpenIssues) {
        this.numberOfOpenIssues = numberOfOpenIssues;
    }

    public int getNumberOfSuccessfulBuilds() {
        return numberOfSuccessfulBuilds;
    }

    public void setNumberOfSuccessfulBuilds(int numberOfSuccessfulBuilds) {
        this.numberOfSuccessfulBuilds = numberOfSuccessfulBuilds;
    }

    public int getNumberOfFailedBuilds() {
        return numberOfFailedBuilds;
    }

    public void setNumberOfFailedBuilds(int numberOfFailedBuilds) {
        this.numberOfFailedBuilds = numberOfFailedBuilds;
    }
}

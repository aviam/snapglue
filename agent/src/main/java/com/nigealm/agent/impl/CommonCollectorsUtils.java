package com.nigealm.agent.impl;

import com.nigealm.common.utils.DateUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.TimeUnit;


public class CommonCollectorsUtils {

    public static final int DAYS_LIMIT = 90;

    private static final char[] issueKeySeparators = {'-', '_'};

    private static final char[] issueKeyDelimiters = {',', ' '};

    private static final String[] mergeCommitPrefixes = {"Merge branch", "Merge remote"};

    private CommonCollectorsUtils() {
    }


    public static List<String> getIssueFromCommitMessage(String message, Set<String> issueKeys) {
        List<String> commitIssueKeys = new LinkedList<>();
        for (String currIssueKey : issueKeys) {
            if (isCommitMessageContainsIssueKey(message, currIssueKey)) {
                commitIssueKeys.add(currIssueKey);
            }
        }
        return commitIssueKeys;
    }

    public static boolean isMergeCommit(String message) {
        for (String currPrefix : mergeCommitPrefixes) {
            if (message.toLowerCase().startsWith(currPrefix.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCommitMessageContainsIssueKey(String message, String issueKey) {
        Pair<String, String> issueProjectNumberPair = splitIssueKeyToProjectAndNumber(issueKey);
        if (issueProjectNumberPair == null) {
            return false;
        }

        for (char currChar : issueKeySeparators) {
            String currIssueKey = issueProjectNumberPair.getLeft() + currChar + issueProjectNumberPair.getRight();
            for (char ch : issueKeyDelimiters) {
                if (isStringContained(message, currIssueKey + ch)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Pair<String, String> splitIssueKeyToProjectAndNumber(String issueKey) {
        String[] projectAndNumberArray = issueKey.split("-");
        if (projectAndNumberArray.length == 2) {
            return new ImmutablePair<>(projectAndNumberArray[0], projectAndNumberArray[1]);
        }
        return null;
    }

    private static boolean isStringContained(String strToSearchIn, String strToBeSearched) {
        return strToSearchIn.toLowerCase().contains(strToBeSearched.toLowerCase());
    }


    public static List<String> getPotentialSprints(Date commitDate, Map<String, Pair<Date, Date>> sprintsDatesMap) {
        List<String> potentialSprints = new LinkedList<>();
        for (Map.Entry<String, Pair<Date, Date>> currEntry : sprintsDatesMap.entrySet()) {
            String sprintName = currEntry.getKey();
            Date startDate = DateUtils.getSameDateInMidnight(currEntry.getValue().getLeft());
            Date endDate = DateUtils.getSameDateEndOfDay(currEntry.getValue().getRight());
            if ((commitDate.after(startDate) || commitDate.equals(startDate)) &&
                    (commitDate.before(endDate) || commitDate.equals(endDate))) {
                potentialSprints.add(sprintName);
            }
        }
        return potentialSprints;
    }

    public static boolean isDateInsideDaysLimit(Date date) {
        if (date == null) {
            return false;
        }

        Calendar calendar = Calendar.getInstance();
        Date todayDate = DateUtils.getSameDateInMidnight(calendar.getTime());

        long diffInMillis = todayDate.getTime() - date.getTime();
        long diffInDays = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);
        if (diffInDays <= DAYS_LIMIT) {
            return true;
        }

        return false;
    }

    public static List<String> getSprintNames(String key, List<String> potentialSprint, Map<String, Set<String>>
            jiraToSprintMap) {
        Set<String> sprintsList = jiraToSprintMap.get(key);

        if (sprintsList == null || sprintsList.isEmpty()) {
            return new LinkedList<>();
        }

        if (potentialSprint == null || potentialSprint.isEmpty()) {
            return new LinkedList<>();
        }

        return ListUtils.intersection(potentialSprint, Arrays.asList
                (sprintsList.toArray(new String[0])));
    }


}

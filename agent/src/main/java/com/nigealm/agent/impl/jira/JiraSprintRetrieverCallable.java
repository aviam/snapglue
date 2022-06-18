package com.nigealm.agent.impl.jira;

import com.nigealm.agent.impl.AgentDataCollectionException;
import com.nigealm.agent.impl.CommonCollectorsUtils;
import com.nigealm.agent.users.UserManager;
import com.nigealm.common.utils.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;

public class JiraSprintRetrieverCallable extends AbstractJiraDataRetriever implements Callable<JSONObject> {
    private int rapidViewId;
    private int sprintId;
    private Map<String, Set<String>> jiraToSprintMap;

    public JiraSprintRetrieverCallable(String projectName, String jiraServerUri, String username, String password,
                                       UserManager userManager, int rapidViewId, int sprintId,
                                       Map<String, Set<String>> jiraToSprintMap) {
        super(projectName, jiraServerUri, username, password, userManager);
        this.rapidViewId = rapidViewId;
        this.sprintId = sprintId;
        this.jiraToSprintMap = jiraToSprintMap;
    }

    @Override
    public JSONObject call() throws Exception {
        String url = jiraServerUri + "/rest/greenhopper/1.0/rapid/charts/sprintreport?" +
                "rapidViewId=" + rapidViewId + "&sprintId=" + sprintId;
        String currSprintIssues = retrieveData(url);
        JSONObject sprintData = new JSONObject(currSprintIssues);
        Date sprintEndDate = getSprintEndDateFromSprintData(sprintData);
        if (sprintEndDate == null || !CommonCollectorsUtils.isDateInsideDaysLimit(sprintEndDate)) {
            return null;
        }

        fetchCompleteIssuesInfo(sprintData);
        buildJiraDateToSprintMap(sprintData, jiraToSprintMap);
        addProjectNameToSprintObject(sprintData);
        return sprintData;
    }

    private Date getSprintEndDateFromSprintData(JSONObject sprintData) throws JSONException {
        JSONObject sprintObj = sprintData.getJSONObject("sprint");
        return DateUtils.parseDate(sprintObj.getString("endDate"));
    }

    private void fetchCompleteIssuesInfo(JSONObject sprintData) throws JSONException, IOException,
            AgentDataCollectionException, JiraRetrieveDataException {
        JSONArray detailedCompletedIssuesArray = new JSONArray();
        JSONArray detailedIncompletedIssuesArray = new JSONArray();
        // get all issues list
        int total = 1;
        int start = 0;
        while (start < total) {
            String url = jiraServerUri + "/rest/api/2/search?jql=sprint=" + sprintId +
                    "&expand=changelog&startAt=" + start;
            String currSprintIssues = retrieveData(url);
            JSONObject issuesWrapObject = new JSONObject(currSprintIssues);
            start += issuesWrapObject.getInt("maxResults");
            total = issuesWrapObject.getInt("total");
            // get full details of the issues
            JSONArray issues = issuesWrapObject.getJSONArray("issues");
            getDetailedIssuesFromArray(issues, detailedCompletedIssuesArray, detailedIncompletedIssuesArray);
        }


        // Create users
        createJiraUsers(detailedIncompletedIssuesArray);
        createJiraUsers(detailedCompletedIssuesArray);

        // Update data on return object
        String incompletedIssuesKey;
        JSONObject contentsObj = sprintData.getJSONObject("contents");
        if (contentsObj.has("incompletedIssues")) {
            incompletedIssuesKey = "incompletedIssues";
        } else {
            incompletedIssuesKey = "issuesNotCompletedInCurrentSprint";
        }
        contentsObj.remove(incompletedIssuesKey);
        contentsObj.put("incompletedIssues", detailedIncompletedIssuesArray);
        contentsObj.put("completedIssues", detailedCompletedIssuesArray);
        contentsObj.put("incompletedIssuesCount", detailedIncompletedIssuesArray.length());
        contentsObj.put("completedIssuesCount", detailedCompletedIssuesArray.length());
    }


    private void getDetailedIssuesFromArray(JSONArray issuesArray, JSONArray detailedCompletedIssuesArray, JSONArray
            detailedIncompletedIssuesArray) throws
            JSONException, IOException,
            AgentDataCollectionException, JiraRetrieveDataException {
        int numberOfIssues = issuesArray.length();
        for (int i = 0; i < numberOfIssues; i++) {
            JSONObject currIssue = issuesArray.getJSONObject(i);
            if (isIssueClosed(currIssue)) {
                detailedCompletedIssuesArray.put(currIssue);
            } else {
                detailedIncompletedIssuesArray.put(currIssue);
            }
        }
    }

    private boolean isIssueClosed(JSONObject detailedIssue) {
        try {
            JSONObject fieldsObject = detailedIssue.getJSONObject("fields");
            JSONObject statusObject = fieldsObject.getJSONObject("status");
            String status = statusObject.getString("name");
            return status.equalsIgnoreCase("Done") || status.equalsIgnoreCase("Closed") ||
                    status.equalsIgnoreCase("Resolved") || status.equalsIgnoreCase("Completed");
        } catch (JSONException e) {
            tracer.exception("isIssueClosed", e);
            return false;
        }
    }

    private void addProjectNameToSprintObject(JSONObject sprintData) throws JSONException {
        JSONObject sprintObject = sprintData.getJSONObject("sprint");
        sprintObject.put("projectName", projectName);
    }

    private void buildJiraDateToSprintMap(JSONObject jiraIssuesData, Map<String, Set<String>> jiraSprintMap)
            throws JSONException {
        JSONObject contentsObj = jiraIssuesData.getJSONObject("contents");
        JSONArray completedIssues = contentsObj.getJSONArray("completedIssues");
        JSONArray inCompletedIssues;
        if (contentsObj.has("incompletedIssues")) {
            inCompletedIssues = contentsObj.getJSONArray("incompletedIssues");
        } else {
            inCompletedIssues = contentsObj.getJSONArray("issuesNotCompletedInCurrentSprint");
        }
        JSONObject sprintInfoObj = jiraIssuesData.getJSONObject("sprint");
        String sprintName = sprintInfoObj.getString("name");

        addIssuesToMap(completedIssues, sprintName, jiraSprintMap);
        addIssuesToMap(inCompletedIssues, sprintName, jiraSprintMap);
    }

    private void addIssuesToMap(JSONArray issuesArray, String sprintName, Map<String,
            Set<String>> jiraSprintMap) throws JSONException {
        for (int i = 0; i < issuesArray.length(); i++) {
            Set<String> sprintsList = new HashSet<>();
            JSONObject currIssue = issuesArray.getJSONObject(i);
            String issueKey = currIssue.getString("key");
            JSONObject changelogObj = currIssue.getJSONObject("changelog");
            if (changelogObj != null) {
                JSONArray historiesArray = changelogObj.getJSONArray("histories");
                for (int j = 0; j < historiesArray.length(); j++) {
                    JSONObject currHistory = historiesArray.getJSONObject(j);
                    JSONArray itemsArray = currHistory.getJSONArray("items");
                    for (int k = 0; k < itemsArray.length(); k++) {
                        JSONObject currItem = itemsArray.getJSONObject(k);
                        String field = currItem.getString("field");
                        if (field.equalsIgnoreCase("sprint")) {
                            String oldSprintName = currItem.getString("fromString");
                            String[] fromArray = oldSprintName.split(",");
                            oldSprintName = fromArray[fromArray.length - 1].trim();

                            String newSprintName = currItem.getString("toString");
                            String[] toArray = newSprintName.split(",");
                            newSprintName = toArray[toArray.length - 1].trim();

                            if (!StringUtils.isEmpty(oldSprintName) && !"null".equals(oldSprintName)) {
                                sprintsList.add(oldSprintName);
                            }

                            if (!newSprintName.equals(oldSprintName)&& !StringUtils.isEmpty
                                    (newSprintName) && !"null".equals(newSprintName)) {
                                sprintsList.add(newSprintName);
                            }
                        }
                    }
                }
            }

            if (sprintsList.isEmpty()) {
                sprintsList.add(sprintName);
            }
            if (!sprintsList.contains(sprintName)) {
                sprintsList.add(sprintName);
            }

            jiraSprintMap.put(issueKey, sprintsList);
        }
    }



    private void createJiraUsers(JSONArray filteredArray) throws JSONException {
        for (int i = 0; i < filteredArray.length(); i++) {
            JSONObject currObject = filteredArray.getJSONObject(i);
            addJiraUserToUsers(currObject);
        }
    }

    private void addJiraUserToUsers(JSONObject currObject) throws JSONException {
        JSONObject fieldsObject = currObject.getJSONObject("fields");
        if (fieldsObject.get("assignee").toString().equals("null")) {
            return;
        }

        JSONObject assigneeObject = fieldsObject.getJSONObject("assignee");
        String fullName = assigneeObject.getString("displayName");
        String email = assigneeObject.getString("emailAddress");
        String jiraUserName = assigneeObject.getString("name");
        boolean isSuccessfullyAdded = userManager.addToolToUserByFullName(fullName, "jira", jiraUserName);
        if (!isSuccessfullyAdded) {
            userManager.createNewUser(fullName, email, jiraUserName);
            userManager.addToolToUserByFullName(fullName, "jira", jiraUserName);
        }
        currObject.put("snapglueUser", fullName);
    }

}

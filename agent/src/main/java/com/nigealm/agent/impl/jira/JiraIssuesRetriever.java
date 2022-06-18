package com.nigealm.agent.impl.jira;

import com.nigealm.agent.impl.AgentDataCollectionException;
import com.nigealm.agent.impl.CommonCollectorsUtils;
import com.nigealm.agent.users.UserManager;
import com.nigealm.common.utils.DateUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.*;

public class JiraIssuesRetriever extends AbstractJiraDataRetriever {

    public JiraIssuesRetriever(String jiraServerUri, String projectName, String username, String password,
                               UserManager userManager) {
        super(projectName, jiraServerUri, username, password, userManager);
        this.jiraServerUri = jiraServerUri;
        this.projectName = projectName;
    }

    public static void main(String args[]) throws URISyntaxException, IOException, ExecutionException, JSONException,
            AgentDataCollectionException, JiraRetrieveDataException {
        JiraIssuesRetriever retriever = new JiraIssuesRetriever("https://equalum.atlassian.net", "Equalum", "avi" +
                ".amsalem",
                "Avshi1212!!", new UserManager());
        retriever.retrieveJIRAData(null, new ConcurrentHashMap<String, Set<String>>(), new HashMap<String,
                Pair<Date, Date>>(), new HashSet<String>());
    }

    public JSONArray retrieveJIRAData(Date lastIterationTime, Map<String, Set<String>> jiraToSprintMap,
                                      Map<String, Pair<Date, Date>> sprintsDatesMap, Set<String> closedSprints) throws
            URISyntaxException, IOException, ExecutionException, JSONException, AgentDataCollectionException,
            JiraRetrieveDataException {
        List<Integer> rapidViewIds = getProjectRapidViewId();
        return getSprintsData(rapidViewIds, lastIterationTime, jiraToSprintMap, sprintsDatesMap, closedSprints);
    }

    private JSONArray getSprintsData(List<Integer> rapidViewIds, Date lastIterationTime, Map<String, Set<String>>
            jiraToSprintMap, Map<String, Pair<Date, Date>> sprintsDatesMap, Set<String> closedSprints) throws
            JSONException, ExecutionException, IOException, AgentDataCollectionException, JiraRetrieveDataException {
        ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
        JSONArray allSprintsData = new JSONArray();
        Set<Integer> activeSprintIdSet = new HashSet<>();
        try {
            for (Integer currRapidViewId : rapidViewIds) {
                String url = jiraServerUri + "/rest/greenhopper/1.0/sprintquery/" + currRapidViewId;
                String viewJSON = retrieveData(url);
                JSONObject viewsObject = new JSONObject(viewJSON);
                JSONArray sprintsArray = (JSONArray) viewsObject.get("sprints");
                List<Future<JSONObject>> sprintReportFutureList = new LinkedList<>();
                for (int i = 0; i < sprintsArray.length(); i++) {
                    JSONObject currSprint = (JSONObject) sprintsArray.get(i);
                    String sprintState = currSprint.getString("state");
                    if ("active".equalsIgnoreCase(sprintState) || DateUtils.isFirstDataCollectionDate(lastIterationTime)) {
                        int sprintId = currSprint.getInt("id");
                        if (activeSprintIdSet.add(sprintId)) {
                            Future<JSONObject> currSprintFuture = executorService.submit(new JiraSprintRetrieverCallable
                                    (projectName, jiraServerUri, username, password, userManager, currRapidViewId, sprintId,
                                            jiraToSprintMap));
                            sprintReportFutureList.add(currSprintFuture);
                        }
                    } else if ("closed".equalsIgnoreCase(sprintState)) {
                        closedSprints.add(currSprint.getString("name"));
                    }
                }
                try {
                    for (Future<JSONObject> sprintFuture : sprintReportFutureList) {
                        JSONObject sprintReport = sprintFuture.get();
                        if (sprintReport == null){
                            continue;
                        }

                        handleSprintReport(sprintReport, sprintsDatesMap, allSprintsData);

                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    tracer.exception("getSprintsData", e);
                }
            }
        } finally {
            executorService.shutdown();
        }
        return allSprintsData;

    }

    private void handleSprintReport(JSONObject sprintReport, Map<String, Pair<Date, Date>>
            sprintsDatesMap, JSONArray allSprintsData) throws JSONException {
        JSONObject sprintObj = sprintReport.getJSONObject("sprint");
        String sprintName = sprintObj.getString("name");
        String startDateStr = sprintObj.getString("startDate");
        String endDateStr = sprintObj.getString("endDate");
        Date startDate = DateUtils.parseDate(startDateStr);
        startDate = DateUtils.getSameDateInMidnight(startDate);
        Date endDate = DateUtils.parseDate(endDateStr);
        endDate = DateUtils.getSameDateEndOfDay(endDate);
        if (CommonCollectorsUtils.isDateInsideDaysLimit(endDate)) {
            sprintsDatesMap.put(sprintName, new ImmutablePair<>(startDate, endDate));
            allSprintsData.put(sprintReport);
        }
    }

    private List<Integer> getProjectRapidViewId() throws JSONException, IOException, AgentDataCollectionException,
            JiraRetrieveDataException {
        if (jiraServerUri.toLowerCase().contains("atlassian.net")) {
            return getProjectRapidViewIdFromCloud();
        } else {
            return getProjectRapidViewIdFromServer();
        }
    }

    private List<Integer> getProjectRapidViewIdFromServer() throws JSONException, IOException,
            AgentDataCollectionException, JiraRetrieveDataException {
        List<Integer> rapidViewsIds = new LinkedList<>();
        String url = jiraServerUri + "/rest/greenhopper/1.0/rapidviews/list";
        String viewJSON = retrieveData(url);
        JSONObject viewsObject = new JSONObject(viewJSON);
        JSONArray viewArray = (JSONArray) viewsObject.get("views");
        for (int i = 0; i < viewArray.length(); i++) {
            JSONObject currView = (JSONObject) viewArray.get(i);
            String viewProjectName = currView.getString("name");
            if (projectName != null && projectName.equalsIgnoreCase(viewProjectName)) {
                int viewId = currView.getInt("id");
                rapidViewsIds.add(viewId);
            }
        }
        return rapidViewsIds;
    }

    private List<Integer> getProjectRapidViewIdFromCloud() throws JSONException, IOException,
            AgentDataCollectionException, JiraRetrieveDataException {
        List<Integer> rapidViewsIds = new LinkedList<>();
        String url = jiraServerUri + "/rest/greenhopper/1.0/rapidviews/list";
        String viewJSON = retrieveData(url);
        JSONObject viewsObject = new JSONObject(viewJSON);
        JSONArray viewArray = (JSONArray) viewsObject.get("views");
        for (int i = 0; i < viewArray.length(); i++) {
            JSONObject currView = (JSONObject) viewArray.get(i);
            int viewId = currView.getInt("id");
            String currProjectUrl = jiraServerUri + "/rest/agile/1.0/board/" + viewId + "/project";
            String currProjectJSON = retrieveData(currProjectUrl);
            JSONObject currProjectObject = new JSONObject(currProjectJSON);
            JSONArray projectValuesArray = currProjectObject.getJSONArray("values");
            String currProjectName = projectValuesArray.getJSONObject(0).getString("name");
            if (projectName != null && projectName.equalsIgnoreCase(currProjectName)) {
                rapidViewsIds.add(viewId);
            }

        }
        return rapidViewsIds;
    }

}

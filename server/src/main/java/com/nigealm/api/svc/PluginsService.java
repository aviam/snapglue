package com.nigealm.api.svc;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.nigealm.ProjectType;
import com.nigealm.agent.svc.AgentDataService;
import com.nigealm.agent.svc.AgentDataServiceImpl.AgentLastExecutionData;
import com.nigealm.alerts.svc.engine.AlertsEngine;
import com.nigealm.common.utils.DateUtils;
import com.nigealm.common.utils.Pair;
import com.nigealm.common.utils.Tracer;
import com.nigealm.mongodb.MongoConnectionManager;
import com.nigealm.mongodb.MongoConnectionManager.MongoDBCollection;
import com.nigealm.mongodb.MongoUtils;
import com.nigealm.utils.Debug;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

import static java.util.Arrays.asList;

/**
 * Servlet implementation class PluginsService
 */
@Component("pluginsService")
public class PluginsService implements org.springframework.web.HttpRequestHandler {
    private final static Tracer tracer = new Tracer(PluginsService.class);

    @Autowired
    private AlertsEngine alertsEngine;

    @Autowired
    private AgentDataService agentDataService;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public PluginsService() {
        super();
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
     * response)
     */

    public void handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // intending to find the last build number in order to save it to the
        // database
        int lastBuildNumber = 0;
        Map<String, Set<String>> projectNameToSprintNames = new HashMap<>();
        try {

            // first get the data from the request
            String agentDataString = getAgentData(request);

            // convert agent data to JSON
            Document agentDataDoc = Document.parse(agentDataString);

            // check if the request is "getLastExecutionData"
            String methodName = MongoUtils.getMethodFromAgentDoc(agentDataDoc);
            if (methodName != null && methodName.equals("getLastExecutionData")) {
                String configID = MongoUtils.getConfigIDFromDoc(agentDataDoc);

                // get agent time
                AgentLastExecutionData agentLastExecutionData = agentDataService.getAgentLastExecutionData(configID);
                String lastExecutionTime = agentLastExecutionData.getLastExecutionTime();
                String lastBuildNumberFromDB = String.valueOf(agentLastExecutionData.getLastBuildNumber());

                tracer.trace("found lastExecutionTime:" + lastExecutionTime + ", found lastBuildNumber: "
                        + lastBuildNumberFromDB);

                response.setContentType("text/html");
                // Actual logic goes here.
                PrintWriter out = response.getWriter();
                out.println("<h1>" + lastExecutionTime + "</h1>");
                out.println("<h2>" + lastBuildNumberFromDB + "</h2>");
                out.flush();
                response.getWriter().write("finished");
                return;
            }

            tracer.info("SnapGlueServer: Processing data from agent...");
            // get the users plugins info
            String configID = MongoUtils.getConfigIDFromDoc(agentDataDoc);
            List<Document> userDocs = MongoUtils.getUserDocs(agentDataDoc);
            List<Document> sprintsMetaDataDocs = MongoUtils.getSprintsMetaDataDocs(agentDataDoc);

            // save or update in db
            handleUsersData(userDocs, configID);
            handleSprintsMetaData(sprintsMetaDataDocs, configID);

            // get the sprint data
            List<Document> sprintDocs = MongoUtils.getSprintDocs(agentDataDoc);
            if (!sprintDocs.isEmpty())
                configID = MongoUtils.getConfigIDFromDoc(agentDataDoc);

            // for each sprint
            for (Document sprintDoc : sprintDocs) {
                Document sprintContent = MongoUtils.getSprintContentFromSprintDoc(sprintDoc);
                Document jiraContent = MongoUtils.getJiraContent(sprintContent);

                if (jiraContent == null)
                    continue;

                Document sprintInfoDoc = MongoUtils.getSprintInfoDocFromSprintDoc(sprintDoc);
                String sprintName = MongoUtils.getSprintNameFromSprintDoc(sprintInfoDoc);
                String projectName = MongoUtils.getProjectNameFromSprintDoc(sprintInfoDoc);
                String sprintState = MongoUtils.getSprintStateFromSprintMetaDataDoc(sprintInfoDoc);

                // save to db the sprint data
                Date now = DateUtils.getDateInUTCMidnight(new Date());
                // Document sprintContent =
                // MongoUtils.getSprintContentFromSprintDoc(sprintDoc);
                // Document jiraContent =
                // MongoUtils.getJiraContent(sprintContent);
                int completedIssuesCount = MongoUtils.getJiraCompletedIssuesCount(jiraContent);
                int incompletedIssuesCount = MongoUtils.getJiraIncompletedIssuesCount(jiraContent);
                int totalIssuesCount = completedIssuesCount + incompletedIssuesCount;

                if ("active".equalsIgnoreCase(sprintState)) {
                    saveOrUpdateSprintMetaData(configID, sprintDoc, now, totalIssuesCount, completedIssuesCount);
                }

                // project to sprint names
                Set<String> sprintNames = projectNameToSprintNames.get(projectName);
                if (sprintNames == null) {
                    sprintNames = new HashSet<>();
                    projectNameToSprintNames.put(projectName, sprintNames);
                }
                sprintNames.add(sprintName);

                boolean isFirstDataCollection = isFirstDataCollection(configID);
                Date sprintStartDate = MongoUtils.getSprintStartDateFromSprintDoc(sprintDoc);
                sprintStartDate = DateUtils.getSameDateInMidnight(sprintStartDate);
                Date sprintEndDate = MongoUtils.getSprintEndDateFromSprintDoc(sprintDoc);
                sprintEndDate = DateUtils.getSameDateInMidnight(sprintEndDate);

                int buildNumber = addContentsToDailyReport(sprintContent, sprintName, projectName, configID,
                        sprintStartDate, sprintEndDate);
                lastBuildNumber = lastBuildNumber > buildNumber ? lastBuildNumber : buildNumber;

                // add history to SPRINTS collection (for each sprint, the
                // number of closed & total issues in each day of the sprint
                // since it started)
                if (isFirstDataCollection)
                    addHistoryToSprintMetaData(configID, sprintDoc);

                tracer.trace("sprint data saved. sprint name / configuration_id: " + sprintName + " / " + configID);
            }

            // get the external data
            Document externalDataDoc = MongoUtils.getExternalDataIfExists(agentDataDoc);
            // save to db the external data
            if (externalDataDoc != null) {
                externalDataDoc.append(MongoUtils.EXTERNAL_DATA_DOC_KEY_INSERT_TIME, new Date().getTime());
                externalDataDoc.append(MongoUtils.DOCUMENT_KEY_CONFIGֹID, configID);
                MongoConnectionManager.addDocument(MongoDBCollection.EXTERNAL_DATA.name(), externalDataDoc);
                tracer.trace("external data saved. configuration_id: " + configID);

                Document sprintContent = MongoUtils.getSprintContentFromSprintDoc(externalDataDoc);
                configID = (configID != null && !configID.equals("none")) ? configID
                        : MongoUtils.getConfigIDFromDoc(sprintContent);
                if (configID == null)
                    configID = "none";
                int buildNumber = addContentsToDailyReport(sprintContent, "External to Sprint", "External to Sprint",
                        configID, new Date(), new Date());
                lastBuildNumber = lastBuildNumber > buildNumber ? lastBuildNumber : buildNumber;
            }

            // save user plug-ins names
            Set<Entry<String, Set<String>>> entrySet = projectNameToSprintNames.entrySet();
            for (Entry<String, Set<String>> entry : entrySet) {
                String projectName = entry.getKey();
                Set<String> sprintNames = entry.getValue();

                FindIterable<Document> projectDocs = MongoConnectionManager
                        .findAllDocsInCollectionByValue(MongoDBCollection.PROJECTS.name(), "projectName", projectName);

                boolean sameconfigID = false;
                Document docFromDB = null;
                if (projectDocs.iterator().hasNext()) {
                    docFromDB = projectDocs.iterator().next();
                    String docConfigID = MongoUtils.getConfigIDFromDoc(docFromDB);
                    sameconfigID = (docConfigID == null && configID == null)
                            || (docConfigID != null && docConfigID.equals(configID));
                }

                // if does not exist in db
                if (!projectDocs.iterator().hasNext() || !sameconfigID) {
                    // save to db the project data
                    Document projectDoc = new Document();
                    projectDoc.append(MongoUtils.PROJCET_DOC_KEY_PROJCET_NAME, projectName);
                    projectDoc.append(MongoUtils.PROJCET_DOC_KEY_SPRINT_NAMES, sprintNames);
                    projectDoc.append(MongoUtils.DOCUMENT_KEY_CONFIGֹID, configID);
                    MongoConnectionManager.addDocument(MongoDBCollection.PROJECTS.name(), projectDoc);
                } else {
                    List<String> sprintNamesFromDB = (ArrayList<String>) docFromDB.get("sprintNames");
                    if (sprintNamesFromDB == null)
                        sprintNamesFromDB = new ArrayList<>();
                    sprintNamesFromDB.addAll(sprintNames);

                    docFromDB.put("sprintNames", sprintNamesFromDB);
                    String id = MongoUtils.getIDFromDoc(docFromDB);

                    // update in db
                    MongoConnectionManager.updateDocument(MongoDBCollection.PROJECTS.name(), docFromDB, id);
                }

                // now compute alert for each sprint
                // for (String sprintName : sprintNames)
                // {
                // computeAlerts(projectName, sprintName);
                // }

            }

            // save agent execution data only if it is the last iteration
            boolean isLastIteration = MongoUtils.isLastIteration(agentDataDoc);
            boolean recievedBuildsData = lastBuildNumber > 0;
            boolean saveToDB = isLastIteration || recievedBuildsData;
            if (saveToDB)
                agentDataService.updateAgentExecutionData(lastBuildNumber, configID, isLastIteration);
        } catch (JSONException e) {
            Debug.trace("cannot read the data sent by the service", e);
            return;
        } catch (Throwable e) {
            Debug.trace("error occurred in handle request: ", e);
            return;
        }
        response.getWriter().write("finished");
    }

    private void addHistoryToSprintMetaData(String configID, Document sprintDoc) {
        Document sprintInfoDoc = MongoUtils.getSprintInfoDocFromSprintDoc(sprintDoc);
        String sprintName = MongoUtils.getSprintNameFromSprintDoc(sprintInfoDoc);

        Date[] allDates = getDatesForSprintHistory(sprintDoc);
        for (Date currentDate : allDates) {
            Date dateUTC = DateUtils.getDateInUTCMidnight(currentDate);

            Pair<Integer, Integer> totalAndCompletedIssuesCount = getTotalAndCompletedIssues(configID, sprintName,
                    dateUTC);
            int totalIssuesCount = totalAndCompletedIssuesCount.getValue1();
            int completedIssuesCount = totalAndCompletedIssuesCount.getValue2();

            saveOrUpdateSprintMetaData(configID, sprintDoc, dateUTC, totalIssuesCount, completedIssuesCount);
        }
    }

    private static Pair<Integer, Integer> getTotalAndCompletedIssues(String configID, String sprintName,
                                                                     Date currentDate) {

        Map<String, List<String>> keyToStatuses = new HashMap<>();

        List<Pair<String, Object>> keysAndValuesList = new ArrayList<>();
        keysAndValuesList.add(new Pair<String, Object>(MongoUtils.DOCUMENT_KEY_SPRINT_NAME, sprintName));
        keysAndValuesList.add(new Pair<String, Object>(MongoUtils.DOCUMENT_KEY_INSERTION_DATE, currentDate));

        // List<Document> issueDocsInDate =
        // MongoConnectionManager.findDocumentsInCollectionByKeysAndValues(
        // MongoDBCollection.ISSUES.name(), keysAndValuesList);

        List<Document> issueDocsInDate = getAllIssuesInSprintForDate(configID, sprintName, currentDate);

        for (Document issueDoc : issueDocsInDate) {
            String status = issueDoc.getString("status");
            String key = issueDoc.getString("_id");

            List<String> statuses = keyToStatuses.get(key);
            if (statuses == null)
                statuses = new ArrayList<>();

            if (!statuses.contains(status))
                statuses.add(status);
            keyToStatuses.put(key, statuses);
        }

        int totalIssuesCount = keyToStatuses.size();
        int completedIssuesCount = 0;
        Set<Entry<String, List<String>>> entrySet = keyToStatuses.entrySet();
        for (Entry<String, List<String>> entry : entrySet) {
            List<String> statuses = entry.getValue();
            boolean isCompleted = false;
            for (String status : statuses) {
                if ((status != null) && (status.equalsIgnoreCase("Done") || status.equalsIgnoreCase("Implemented")
                        || status.equalsIgnoreCase("Fixed") || status.equalsIgnoreCase("Closed"))) {
                    isCompleted = true;
                    break;
                }
            }
            if (isCompleted)
                completedIssuesCount++;
        }

        return new Pair<Integer, Integer>(totalIssuesCount, completedIssuesCount);
    }

    private static List<Document> getAllIssuesInSprintForDate(String configID, String sprintName, Date date) {
        Date d = DateUtils.getDateInUTCEndOfDay(date);
        List<Document> matchList = asList(new Document("$match",
                new Document("configuration_id", configID).append(
                        "sprintName", sprintName).append(
                        "lastUpdate", new Document("$lte", d)
                )));
        List<Document> sortList = asList(new Document("$sort",
                new Document("lastUpdate", -1).append("limit", 1)));

        List<Document> groupByList = asList(new Document("$group",
                new Document("_id", "$key").append(
                        "status", new Document("$first", "$status"))
        ));

        List<Document> fieldsToAggregate = new LinkedList<>();
        fieldsToAggregate.addAll(matchList);
        fieldsToAggregate.addAll(sortList);
        fieldsToAggregate.addAll(groupByList);
        List<Document> res = MongoConnectionManager.doAggregationOnCollection(MongoDBCollection.ISSUES.name(),
                fieldsToAggregate);
        return res;
    }

    private static boolean isFirstDataCollection(String configID) {
        FindIterable<Document> lastExecutionDocs = MongoConnectionManager.findAllDocsInCollectionByValue(
                MongoDBCollection.AGENT_EXECUTION_META_DATA.name(), MongoUtils.DOCUMENT_KEY_CONFIGֹID, configID);

        // if exist
        Date startOfTime = new Date(0);
        long lastExecutionTimeInMillisFromDB = startOfTime.getTime();
        if (lastExecutionDocs != null && lastExecutionDocs.iterator().hasNext()) {
            // get the data from db...
            Document lastExecutionDoc = lastExecutionDocs.iterator().next();
            String lastExecutionTimeInMillisStr = MongoUtils
                    .getLastSuccessTimeFromAgentExecutionDataDoc(lastExecutionDoc);
            lastExecutionTimeInMillisFromDB = Long.parseLong(lastExecutionTimeInMillisStr);
        }

        Date dateInDB = new Date(lastExecutionTimeInMillisFromDB);
        Calendar c = Calendar.getInstance();
        c.set(2016, 3, 1);
        Date d = c.getTime();
        boolean isFirstDataCollection = dateInDB.before(d);
        return isFirstDataCollection;
    }

    private static void saveOrUpdateSprintMetaData(String configID, Document sprintDoc, Date forDate,
                                                   int totalIssuesCount, int completedIssuesCount) {
        Document sprintInfoDoc = MongoUtils.getSprintInfoDocFromSprintDoc(sprintDoc);
        String sprintName = MongoUtils.getSprintNameFromSprintDoc(sprintInfoDoc);
        String projectName = MongoUtils.getProjectNameFromSprintDoc(sprintInfoDoc);

        Date sprintStartDate = MongoUtils.getSprintStartDateFromSprintDoc(sprintDoc);
        Date sprintEndDate = MongoUtils.getSprintEndDateFromSprintDoc(sprintDoc);
        String sprintState = MongoUtils.getSprintStateFromSprintMetaDataDoc(sprintInfoDoc);

        BasicDBObject filter = new BasicDBObject();
        filter.append(MongoUtils.DOCUMENT_KEY_CONFIGֹID, configID);
        filter.append(MongoUtils.DOCUMENT_KEY_SPRINT_NAME, sprintName);

        List<Document> allSprintFromDB = MongoConnectionManager
                .findDocumentsInCollectionByFilter(MongoDBCollection.SPRINTS.name(), filter);
        Document sprintDocFromDB = allSprintFromDB.isEmpty() ? null : allSprintFromDB.iterator().next();

        // is found in db?
        if (sprintDocFromDB != null) {
            // exists in db
            List<Document> dailyData = (List<Document>) sprintDocFromDB.get(MongoUtils.SPRINT_DOC_KEY_DAILY_DATA);
            Iterator<Document> iterator = dailyData.iterator();

            // look for the data of the current date if exists
            Document dailyDataDocFound = null;
            while (iterator.hasNext()) {
                Document currentDailyDataDoc = (Document) iterator.next();
                Date currentDailyDataDate = currentDailyDataDoc.getDate(MongoUtils.DAILY_DATA_DOC_KEY_DATE);
                if (forDate.equals(currentDailyDataDate)) {
                    dailyDataDocFound = currentDailyDataDoc;
                    break;
                }
            }
            if (dailyDataDocFound != null) {
                // found, update
                dailyDataDocFound.put(MongoUtils.SPRINT_DOC_KEY_TOTAL_ISSUES_COUNT, totalIssuesCount);
                dailyDataDocFound.put(MongoUtils.SPRINT_DOC_KEY_CLOSED_ISSUES_COUNT, completedIssuesCount);
                String id = MongoUtils.getIDFromDoc(sprintDocFromDB);

                // set the state as active
                sprintDocFromDB.put(MongoUtils.SPRINT_DOC_KEY_STATE, sprintState);

                // update in db
                MongoConnectionManager.updateDocument(MongoDBCollection.SPRINTS.name(), sprintDocFromDB, id);
            } else {
                // not found, create new
                Document newDailyDataDoc = new Document();
                newDailyDataDoc.append(MongoUtils.DAILY_DATA_DOC_KEY_DATE, forDate)
                        .append(MongoUtils.SPRINT_DOC_KEY_TOTAL_ISSUES_COUNT, totalIssuesCount)
                        .append(MongoUtils.SPRINT_DOC_KEY_CLOSED_ISSUES_COUNT, completedIssuesCount);
                dailyData.add(newDailyDataDoc);

                // set the state as active
                sprintDocFromDB.put(MongoUtils.SPRINT_DOC_KEY_STATE, sprintState);

                String id = MongoUtils.getIDFromDoc(sprintDocFromDB);

                // update in db
                MongoConnectionManager.updateDocument(MongoDBCollection.SPRINTS.name(), sprintDocFromDB, id);
            }
        } else {
            // not found in db
            Document sprintDataDoc = new Document();
            sprintDataDoc.append(MongoUtils.DOCUMENT_KEY_CONFIGֹID, configID);
            sprintDataDoc.append(MongoUtils.DOCUMENT_KEY_SPRINT_NAME, sprintName);
            sprintDataDoc.append(MongoUtils.DOCUMENT_KEY_PROJECT_NAME, projectName);
            sprintDataDoc.append(MongoUtils.SPRINT_DOC_KEY_SPRINT_START_DATE, DateUtils.getDateInUTC(sprintStartDate));
            sprintDataDoc.append(MongoUtils.SPRINT_DOC_KEY_SPRINT_END_DATE, DateUtils.getDateInUTC(sprintEndDate));
            sprintDataDoc.append(MongoUtils.SPRINT_DOC_KEY_STATE, sprintState);

            List<Document> newDailyDataArray = new ArrayList<>();

            Document sprintDailyDataDoc = new Document();
            sprintDailyDataDoc.append(MongoUtils.DAILY_DATA_DOC_KEY_DATE, forDate)
                    .append(MongoUtils.SPRINT_DOC_KEY_TOTAL_ISSUES_COUNT, totalIssuesCount)
                    .append(MongoUtils.SPRINT_DOC_KEY_CLOSED_ISSUES_COUNT, completedIssuesCount);

            newDailyDataArray.add(sprintDailyDataDoc);
            sprintDataDoc.append(MongoUtils.SPRINT_DOC_KEY_DAILY_DATA, newDailyDataArray);

            // save in db
            MongoConnectionManager.addDocument(MongoDBCollection.SPRINTS.name(), sprintDataDoc);
        }
    }

    /**
     * handles the given content
     *
     * @param sprintContent might be null
     * @param sprintName    might be null
     * @param projectName   might be null
     * @return the last build number (greatest)
     */
    private int addContentsToDailyReport(Document sprintContent, String sprintName, String projectName, String configID,
                                         Date sprintStartDate, Date sprintEndDate) {
        /** JIRA */
        handleJiraData(sprintContent, sprintName, projectName, configID, sprintStartDate, sprintEndDate);

        /** JENKINS */
        List<Document> jenkinsDocs = MongoUtils.getJenkinsDocsFromSprintContentDoc(sprintContent);
        int lastBuildNumber = 0;
        for (Document jenkinsDoc : jenkinsDocs) {
            // get the build number
            int number = MongoUtils.getJenkinsBuildNumber(jenkinsDoc);
            lastBuildNumber = lastBuildNumber > number ? lastBuildNumber : number;

            addJenkinsDataToDailyReport(configID, sprintName, projectName, jenkinsDoc, number);
        }

        /** BAMBOO */
        List<Document> bambooDocs = MongoUtils.getBambooDocs(sprintContent);
        for (Document bambooDoc : bambooDocs) {
            // get the build number
            // int number = MongoUtils.getBambooBuildNumber(bambooDoc);
            // lastBuildNumber = lastBuildNumber > number ? lastBuildNumber :
            // number;
            //
            // addBambooDataToDailyReport(configID, sprintName, projectName,
            // bambooDoc);
        }

        /** GITLAB */
        List<Document> gitlabDocs = MongoUtils.getGitlabDocs(sprintContent);
        for (Document gitlabDoc : gitlabDocs) {
            addGitlabDataToDailyReport(configID, sprintName, projectName, gitlabDoc);
        }

        /** GITHUB */
        List<Document> githubDocs = MongoUtils.getGithubDocs(sprintContent);
        for (Document githubDoc : githubDocs) {
            addGithubDataToDailyReport(configID, sprintName, projectName, githubDoc);
        }

        /** BITBUCKET */
        List<Document> bitbucketDocs = MongoUtils.getBitbucketDocs(sprintContent);
        for (Document bitbucketDoc : bitbucketDocs) {
            addBitbucketDataToDailyReport(configID, sprintName, projectName, bitbucketDoc);
        }

        return lastBuildNumber;
    }

    private void handleJiraData(Document sprintContent, String sprintName, String projectName, String configID, Date
            sprintStartDate, Date sprintEndDate) {
        Document jiraContent = MongoUtils.getJiraContent(sprintContent);
        if (jiraContent == null) {
            return;
        }
        List<Document> jiraDocsCompletedIssues = MongoUtils.getJiraDocsCompletedIssues(jiraContent);
        List<String> issueKeysList = new LinkedList<>();
        for (Document issueDoc : jiraDocsCompletedIssues) {
            String key = MongoUtils.getIssueKeyFromIssueDoc(issueDoc);
            addJiraIssueDataToDailyReport(configID, sprintName, projectName, issueDoc,
                    sprintStartDate, sprintEndDate);
            issueKeysList.add(key);
        }

        List<Document> jiraDocsIncompletedIssues = MongoUtils.getJiraDocsIncompletedIssues(jiraContent);
        for (Document issueDoc : jiraDocsIncompletedIssues) {
            String key = MongoUtils.getIssueKeyFromIssueDoc(issueDoc);
            addJiraIssueDataToDailyReport(configID, sprintName, projectName, issueDoc,
                    sprintStartDate, sprintEndDate);
            issueKeysList.add(key);
        }

        List<Document> sprintIssues = getIssuesFromDBBySprintName(configID, sprintName);
        for (Document issueDoc : sprintIssues){
            String key = MongoUtils.getIssueKeyFromIssueDoc(issueDoc);
            if (!issueKeysList.contains(key)){
                Date lastUpdate = new Date();
                String assignee = issueDoc.getString(MongoUtils.ISSUE_DOC_KEY_ASSIGNEE);
                String status = issueDoc.getString(MongoUtils.ISSUE_DOC_KEY_STATUS);
                String executionDate = DateUtils.getExecutionDate(lastUpdate);
                SprintHistoryChange sprintChange = new SprintHistoryChange(lastUpdate, DateUtils
                        .getExecutionDate(lastUpdate), "");
                updateExitingIssueDocument(configID, "", assignee, status, key, lastUpdate, executionDate);
                Document updatedIssueDoc = getIssueFromDBByKey(configID, key);
                List<Document> sprintsData = MongoUtils.getIssueSprintsDataFromIssueDoc(updatedIssueDoc);
                addSprintHistoryDataToList(sprintChange, sprintsData);
                Collections.sort(sprintsData, new DocumentDateComparator(MongoUtils.SPRINT_HISTORY_DOC_KEY_ADDED_AT));

                // update in db
                String id = MongoUtils.getIDFromDoc(updatedIssueDoc);
                long start = System.currentTimeMillis();
                MongoConnectionManager.updateDocument(MongoDBCollection.ISSUES.name(), updatedIssueDoc, id);
                long end = System.currentTimeMillis();
                tracer.trace("Update document in DB took: " + (end - start) + " Milliseconds");
            }
        }

    }

    /**
     * Saves the user names in plug-ins to the database
     *
     * @param userDocs might be null
     * @throws JSONException
     */
    private void handleUsersData(List<Document> userDocs, String configID) throws JSONException {
        for (Document userDoc : userDocs) {
            MongoUtils.saveOrUpdateUser(userDoc, configID);
        }
    }

    private void handleSprintsMetaData(List<Document> sprintsMetaDataDocs, String configID) throws JSONException {
        for (Document doc : sprintsMetaDataDocs) {
            String sprintState = MongoUtils.getSprintStateFromSprintMetaDataDoc(doc);
            String sprintName = MongoUtils.getSprintNameFromSprintMetaDataDoc(doc);

            BasicDBObject filter = new BasicDBObject();
            filter.append(MongoUtils.DOCUMENT_KEY_CONFIGֹID, configID);
            filter.append(MongoUtils.DOCUMENT_KEY_SPRINT_NAME, sprintName);

            List<Document> allSprintFromDB = MongoConnectionManager
                    .findDocumentsInCollectionByFilter(MongoDBCollection.SPRINTS.name(), filter);
            Document sprintDoc = allSprintFromDB.isEmpty() ? null : allSprintFromDB.iterator().next();
            if (sprintDoc == null)
                continue;

            String id = MongoUtils.getIDFromDoc(sprintDoc);

            // update the sprint doc
            sprintDoc.put(MongoUtils.SPRINT_DOC_KEY_STATE, sprintState);

            // update in db
            MongoConnectionManager.updateDocument(MongoDBCollection.SPRINTS.name(), sprintDoc, id);
        }
    }

    private static String getAgentData(HttpServletRequest request) {
        String line;
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null)
                sb.append(line);
        } catch (Exception e) {
            /* report an error */
            Debug.trace(e);
        }
        return sb.toString();
    }

    private void addJiraIssueDataToDailyReport(String configID, String sprintName, String projectName,
                                               Document jiraIssueDoc,Date sprintStartDate, Date
                                                       sprintEndDate) {
        Document fieldsDoc = ((Document) jiraIssueDoc.get("fields"));

        String assignee = MongoUtils.getSnapGlueUserFromDoc(jiraIssueDoc);
        if (assignee == null)
            assignee = MongoUtils.getIssueAssigneeFromFieldsDoc(fieldsDoc);
        String description = fieldsDoc.getString("summary");
        String type = ProjectType.ISSUES.name();
        String lastUpdate = fieldsDoc.getString("updated");
        String status = ((Document) fieldsDoc.get("status")).getString("name");
        String url = jiraIssueDoc.getString("self");
        String key = MongoUtils.getIssueKeyFromJiraDoc(jiraIssueDoc);
        int index = url.indexOf("/rest");
        url = url.substring(0, index) + "/browse/" + key;

        // we want to save in db only the issues that their status was changed
        // (comparing to the last updated) or its sprint was changed
        FindIterable<Document> documents = MongoConnectionManager
                .findAllDocsInCollectionByValue(MongoDBCollection.DAILYREPORTELEMENTS.name(), "url", url);
        MongoCursor<Document> cursor = documents.iterator();

        // filter by configuration id
        List<Document> documentsFiltered = new ArrayList<>();
        while (cursor.hasNext()) {
            Document currentDoc = cursor.next();
            String configurationId = currentDoc.getString("configuration_id");
            if (configurationId != null && configurationId.equals(configID))
                documentsFiltered.add(currentDoc);
        }

        boolean shouldCreateDailyDocument = shouldCreateDailyDocument(sprintName, assignee, status, documentsFiltered);

        if (shouldCreateDailyDocument) {
            addDailyDocument(configID, sprintName, projectName, assignee, url, lastUpdate, status, type, description);
        }

        addIssueDocument(configID, sprintName, projectName, assignee, url, lastUpdate, status, key, jiraIssueDoc,
                sprintStartDate, sprintEndDate);
    }

    private boolean shouldCreateDailyDocument(String sprintName, String assignee, String status, List<Document>
            documentsFiltered) {
        Iterator<Document> cursor1 = documentsFiltered.iterator();

        Document lastUpdatedDoc = cursor1.hasNext() ? cursor1.next() : null;
        String lastStatus = null;
        String lastSprintName = null;
        String lastAssignee = null;
        if (lastUpdatedDoc != null) {
            // look for the one which was last updated
            while (cursor1.hasNext()) {
                Document currentDoc = cursor1.next();
                String lastUpdateStrCurrent = getLastUpdatedFromDoc(currentDoc);
                String lastUpdateStrPrevious = getLastUpdatedFromDoc(lastUpdatedDoc);
                Date lastUpdateDateCurrent = DateUtils.formatDateStringToDate(lastUpdateStrCurrent);
                Date lastUpdateDatePrevious = DateUtils.formatDateStringToDate(lastUpdateStrPrevious);

                if (lastUpdateDateCurrent == null) {
                    // no update time, go to next one
                    continue;
                }
                if (lastUpdateDatePrevious == null) {
                    // no update time, go to next one
                    lastUpdatedDoc = currentDoc;
                    continue;
                }

                // both dates exist, check for the later one
                lastUpdatedDoc = lastUpdateDateCurrent.after(lastUpdateDatePrevious) ? currentDoc : lastUpdatedDoc;
            }
            // found the last updated, check status & sprint & assignee (skip if
            // they are all the same)
            lastStatus = lastUpdatedDoc.getString("status");
            lastSprintName = lastUpdatedDoc.getString("sprintName");
            lastAssignee = lastUpdatedDoc.getString("assignee");

        }

        boolean shouldCreateDailyDocument = false;
        if (!((status == null || status.equals(lastStatus))
                && (lastSprintName == null || lastSprintName.equals(sprintName))
                && (assignee == null || assignee.equals(lastAssignee)))) {
            shouldCreateDailyDocument = true;
        }

        return shouldCreateDailyDocument;
    }

    private String getLastUpdatedFromDoc(Document issueDoc) {
        if (issueDoc == null) {
            return null;
        }

        if (issueDoc.containsKey("updated")) {
            return issueDoc.getString("updated");
        }

        if (issueDoc.containsKey("lastUpdate")) {
            return issueDoc.getString("lastUpdate");
        }
        tracer.warn("Last updated date not found for issue: " + issueDoc.get("key"));
        return null;
    }

    private void addBambooDataToDailyReport(String configID, String sprintName, String projectName, Document
            bambooDoc) {
        // String executor = bambooDoc.getString("buildReason");
        // JSONObject linkJson = bambooDoc.get("link", JSONObject.class);
        // String url = "";
        // try
        // {
        // url = linkJson.getString("href");
        // }
        // catch (JSONException e)
        // {
        // e.printStackTrace();
        // }
        // String lastUpdate = bambooDoc.getString("buildCompletedTime");
        // String status = bambooDoc.getString("state");
        // String type = ProjectType.BUILDS.name();
        //
        // String successfulTestCount =
        // bambooDoc.getString("successfulTestCount");
        // String failedTestCount = bambooDoc.getString("failedTestCount");
        // String skippedTestCount = bambooDoc.getString("skippedTestCount");
        // String description = "Successful Tests: " + successfulTestCount + ",
        // Failed Tests: "
        // + failedTestCount + ", Tests Skipped: " + skippedTestCount;
        //
        // addDailyDocument(configID, sprintName, projectName, executor, url,
        // lastUpdate, status, type, description);
        // addBuildDocument(configID, sprintName, projectName, executor, url,
        // lastUpdate, status, description);
    }

    private void addGitlabDataToDailyReport(String configID, String sprintName, String projectName, Document
            gitlabDoc) {
        String executor = MongoUtils.getSnapGlueUserFromDoc(gitlabDoc);
        if (executor == null)
            executor = MongoUtils.getCommitExecutorFromCommitDoc(gitlabDoc);
        String url = gitlabDoc.getString("");
        String lastUpdate = gitlabDoc.getString("created_at");
        String status = gitlabDoc.getString("");
        String type = ProjectType.COMMIT.name();
        String description = gitlabDoc.getString("title");

        ArrayList<String> relatedIssues = MongoUtils.getRelatedIssueFromCommitDoc(gitlabDoc);
        int relatedBuildNumber = MongoUtils.getRelatedBuildFromCommitDoc(gitlabDoc);

        addDailyDocument(configID, sprintName, projectName, executor, url, lastUpdate, status, type, description);
        addCommitDocument(configID, sprintName, projectName, executor, url, lastUpdate, status, description,
                relatedIssues, relatedBuildNumber);
    }

    private void addGithubDataToDailyReport(String configID, String sprintName, String projectName, Document
            githubDoc) {
        String executor = MongoUtils.getSnapGlueUserFromDoc(githubDoc);
        if (executor == null)
            executor = MongoUtils.getCommitterFromGithubDoc(githubDoc);
        String url = githubDoc.getString("");
        String lastUpdate = MongoUtils.getExecutionDateFromGithubDoc(githubDoc);
        String status = githubDoc.getString("");
        String type = ProjectType.COMMIT.name();
        String description = MongoUtils.getDescriptionFromGithubDoc(githubDoc);

        ArrayList<String> relatedIssues = MongoUtils.getRelatedIssueFromCommitDoc(githubDoc);
        int relatedBuildNumber = MongoUtils.getRelatedBuildFromCommitDoc(githubDoc);

        addDailyDocument(configID, sprintName, projectName, executor, url, lastUpdate, status, type, description);
        addCommitDocument(configID, sprintName, projectName, executor, url, lastUpdate, status, description,
                relatedIssues, relatedBuildNumber);
    }

    private void addBitbucketDataToDailyReport(String configID, String sprintName, String projectName,
                                               Document bitbucketDoc) {
        String executor = MongoUtils.getSnapGlueUserFromDoc(bitbucketDoc);
        if (executor == null)
            executor = MongoUtils.getCommitterFromBitbucketDoc(bitbucketDoc);
        String url = bitbucketDoc.getString("");
        String lastUpdate = MongoUtils.getExecutionDateFromBitbucketDoc(bitbucketDoc);
        String status = bitbucketDoc.getString("");
        String type = ProjectType.COMMIT.name();
        String description = MongoUtils.getDescriptionFromBitbucketDoc(bitbucketDoc);

        ArrayList<String> relatedIssues = MongoUtils.getRelatedIssueFromCommitDoc(bitbucketDoc);
        int relatedBuildNumber = MongoUtils.getRelatedBuildFromCommitDoc(bitbucketDoc);

        addDailyDocument(configID, sprintName, projectName, executor, url, lastUpdate, status, type, description);
        addCommitDocument(configID, sprintName, projectName, executor, url, lastUpdate, status, description,
                relatedIssues, relatedBuildNumber);
    }

    private void addJenkinsDataToDailyReport(String configID, String sprintName, String projectName, Document buildDoc,
                                             int number) {
        String executor = MongoUtils.getSnapGlueUserFromDoc(buildDoc);
        if (executor == null)
            executor = MongoUtils.getBuildExecutorFromBuildDoc(buildDoc);
        String url = buildDoc.getString("url");
        long timestamp = buildDoc.getLong("timestamp");
        String lastUpdate = new Date(timestamp).toString();
        String status = buildDoc.getString("result");
        String type = ProjectType.BUILDS.name();

        List<Document> actions = (List) buildDoc.get("actions");
        Integer failCount = null;
        Integer totalCount = null;
        Integer skipCount = null;
        for (Document action : actions) {
            failCount = action.getInteger("failCount");

            if (failCount != null) {
                totalCount = action.getInteger("totalCount");
                skipCount = action.getInteger("skipCount");
                break;
            }
        }

        int total = totalCount != null ? totalCount : -1;
        int failures = failCount != null ? failCount : -1;
        int skipped = skipCount != null ? skipCount : -1;

        int successCount = total - failures - skipped;
        String description = "Successful Tests: " + successCount + ", Failed Tests: " + failures + ", Tests Skipped: "
                + skipped;

        addDailyDocument(configID, sprintName, projectName, executor, url, lastUpdate, status, type, description);
        addBuildDocument(configID, sprintName, projectName, executor, url, lastUpdate, status, description, number);
    }

    private void addDailyDocument(String configID, String sprintName, String projectName, String assignee, String url,
                                  String lastUpdate, String status, String type, String description) {
        Document dailyDoc = new Document();

        Date lastUpdateDate = DateUtils.formatDateStringToDate(lastUpdate);
        lastUpdate = lastUpdateDate.toString();

        java.sql.Date executionDate = new java.sql.Date(lastUpdateDate.getYear(), lastUpdateDate.getMonth(),
                lastUpdateDate.getDate());

        String dateString = executionDate.toString();

        dailyDoc.append(MongoUtils.DOCUMENT_KEY_CONFIGֹID, configID);
        dailyDoc.append("sprintName", sprintName);
        dailyDoc.append("projectName", projectName);
        dailyDoc.append("assignee", assignee);
        dailyDoc.append("url", url);
        dailyDoc.append("lastUpdate", lastUpdate);
        dailyDoc.append("status", status);
        dailyDoc.append("type", type);
        dailyDoc.append("description", description);
        dailyDoc.append("executionDate", dateString);

        MongoConnectionManager.addDocument(MongoDBCollection.DAILYREPORTELEMENTS.name(), dailyDoc);
        // tracer.trace("finished creating dailyReportElement: " +
        // dailyDoc.toString());
    }

    private void addBuildDocument(String configID, String sprintName, String projectName, String executor, String url,
                                  String lastUpdate, String status, String description, int number) {
        Document dailyDoc = new Document();

        Date lastUpdateDate = DateUtils.formatDateStringToDate(lastUpdate);
        java.sql.Date executionDate = new java.sql.Date(lastUpdateDate.getYear(), lastUpdateDate.getMonth(),
                lastUpdateDate.getDate());
        String dateString = executionDate.toString();

        dailyDoc.append(MongoUtils.DOCUMENT_KEY_CONFIGֹID, configID);
        dailyDoc.append(MongoUtils.BUILD_DOC_KEY_BUILD_NUMBER, Integer.valueOf(number));
        dailyDoc.append("sprintName", sprintName);
        dailyDoc.append("projectName", projectName);
        dailyDoc.append("executor", executor);
        dailyDoc.append("url", url);
        dailyDoc.append("lastUpdate", DateUtils.getDateInUTC(lastUpdateDate));
        dailyDoc.append("status", status);
        dailyDoc.append("description", description);
        dailyDoc.append("executionDate", dateString);

        MongoConnectionManager.addDocument(MongoDBCollection.BUILDS.name(), dailyDoc);
        tracer.trace("finished creating dailyReportElement: " + dailyDoc.toString());
    }

    private void addIssueDocument(String configID, String sprintName, String projectName, String assignee, String url,
                                  String lastUpdate, String status, String key, Document jiraIssueDoc, Date
                                          sprintStartDate, Date sprintEndDate) {
        assignee = (StringUtils.isEmpty(assignee)) ? "Unassigned" : assignee;
        Date lastUpdateDate = DateUtils.formatDateStringToDate(lastUpdate);
        String executionDate = DateUtils.getExecutionDate(lastUpdateDate);
        Document issueFromDB = getIssueFromDBByKey(configID, key);
        Date currLastUpdate = null;
        if (issueFromDB == null) {
            createIssueDocument(configID, sprintName, projectName, key, url, status, assignee, lastUpdateDate,
                    executionDate);
            issueFromDB = getIssueFromDBByKey(configID, key);
        } else {
            currLastUpdate = issueFromDB.getDate(MongoUtils.ISSUE_DOC_KEY_LAST_UPDATE);
            issueFromDB = updateExitingIssueDocument(configID, sprintName, assignee, status, key, lastUpdateDate,
                    executionDate);
        }

        updateHistoryForIssue(sprintName, assignee, status, jiraIssueDoc,
                sprintStartDate, sprintEndDate, lastUpdateDate, issueFromDB, currLastUpdate);

    }

    private void updateHistoryForIssue(String sprintName, String assignee, String status, Document jiraIssueDoc,
                                       Date sprintStartDate, Date sprintEndDate, Date lastUpdateDate,
                                       Document issueFromDB, Date currLastUpdate) {

        LinkedList<SprintHistoryChange> sprintsHistoryChanges = new LinkedList<>();
        LinkedList<StatusHistoryChange> statusHistoryChanges = new LinkedList<>();
        LinkedList<AssigneeHistoryChange> assigneeHistoryChanges = new LinkedList<>();
        long start;
        long end;
        extractHistoryFromIssue(issueFromDB, jiraIssueDoc, sprintStartDate, sprintEndDate, currLastUpdate,
                sprintsHistoryChanges,
                statusHistoryChanges, assigneeHistoryChanges, sprintName, assignee, status, lastUpdateDate);


        start = System.currentTimeMillis();
        saveIssueHistoryToDB(issueFromDB, sprintsHistoryChanges, statusHistoryChanges, assigneeHistoryChanges);
        end = System.currentTimeMillis();
        tracer.trace("Save History to DB took: " + (end - start) + " Milliseconds");

    }

    private void saveIssueHistoryToDB(Document issueFromDB, LinkedList<SprintHistoryChange> sprintsHistoryChanges,
                                      LinkedList<StatusHistoryChange> statusHistoryChanges,
                                      LinkedList<AssigneeHistoryChange> assigneeHistoryChanges) {

        boolean dataSaved = false;
        List<Document> sprintsData = MongoUtils.getIssueSprintsDataFromIssueDoc(issueFromDB);
        for (SprintHistoryChange currChange : sprintsHistoryChanges) {
            addSprintHistoryDataToList(currChange, sprintsData);
            dataSaved = true;
        }

        List<Document> statusesData = MongoUtils.getIssueStatusesDataFromIssueDoc(issueFromDB);
        for (StatusHistoryChange currChange : statusHistoryChanges) {
            addStatusHistoryDataToIssue(currChange, statusesData);
            dataSaved = true;
        }

        List<Document> assigneesData = MongoUtils.getIssueAssigneesDataFromIssueDoc(issueFromDB);
        for (AssigneeHistoryChange currChange : assigneeHistoryChanges) {
            addAssigneeHistoryDataToIssue(currChange, assigneesData);
            dataSaved = true;
        }


        if (dataSaved){
            Collections.sort(sprintsData, new DocumentDateComparator(MongoUtils.SPRINT_HISTORY_DOC_KEY_ADDED_AT));
            Collections.sort(statusesData, new DocumentDateComparator(MongoUtils.STATUS_HISTORY_DOC_KEY_CHANGED_AT));
            Collections.sort(assigneesData, new DocumentDateComparator(MongoUtils.ASSIGNEE_HISTORY_DOC_KEY_ASSIGNED_AT));

            // update in db
            String id = MongoUtils.getIDFromDoc(issueFromDB);
            long start = System.currentTimeMillis();
            MongoConnectionManager.updateDocument(MongoDBCollection.ISSUES.name(), issueFromDB, id);
            long end = System.currentTimeMillis();
            tracer.trace("Update document in DB took: " + (end - start) + " Milliseconds");
        }
    }

    private void extractHistoryFromIssue(Document issueFromDB, Document jiraIssueDoc, Date sprintStartDate, Date
            sprintEndDate, Date currLastUpdate, LinkedList<SprintHistoryChange> sprintsHistoryChanges,
                                         LinkedList<StatusHistoryChange> statusHistoryChanges,
                                         LinkedList<AssigneeHistoryChange> assigneeHistoryChanges,
                                         String sprintName, String assignee, String status, Date lastUpdateDate) {
//        Date historyStartDate = issueCreationDate.after(sprintStartDate) ? issueCreationDate : sprintStartDate;
//        historyStartDate = (currLastUpdate == null) ? historyStartDate : currLastUpdate;
//        Date historyEndDate = lastUpdateDate.after(sprintStartDate) ? lastUpdateDate : sprintStartDate;
//        historyEndDate = historyEndDate.after(sprintEndDate) ? sprintEndDate : historyEndDate;
        // start (0) , 1, 2 , 3 (change) , 4 , 5 , 6 (change) , 7, 8 (last update-start history), 9 (yesterday) , 10
        // (today)
//        Date[] datesDescending = DateUtils.getAllDatesBetweenDates(historyStartDate, historyEndDate, false).toArray
//                (new Date[]{});
//        for (int i = 0; i < datesDescending.length; i++) {
        // first create the history data for now
//            Date currentDate = datesDescending[i];

        // now get other history data for this day
        List<Document> historyDocsForCurrentDate = MongoUtils.getHistoryDocsFromIssueDoc(jiraIssueDoc);


        for (Iterator iterator = historyDocsForCurrentDate.iterator(); iterator.hasNext(); ) {
            Document historyDoc = (Document) iterator.next();
            String updateDate = MongoUtils.getHistoryDateFromHistoryDoc(historyDoc);
            Date historyLastUpdateDate = DateUtils.parseDate(updateDate);
            if (currLastUpdate != null && historyLastUpdateDate.before(currLastUpdate)) {
                continue;
            }

            String prevSprintName = handleSprintHistoryChanges(sprintsHistoryChanges, historyDoc,
                    historyLastUpdateDate);

            String prevStatus = handleStatusHistoryChanges(statusHistoryChanges, historyDoc,
                    historyLastUpdateDate);

            String prevAssignee = handleAssigneeHistoryChanges(assigneeHistoryChanges, historyDoc,
                    historyLastUpdateDate);

            if (prevSprintName == null && prevStatus == null && prevAssignee == null)
                continue;
        }

//        }

//        if (sprintsHistoryChanges.isEmpty()) {
//            sprintsHistoryChanges.add(new SprintHistoryChange(sprintStartDate, DateUtils
//                    .getExecutionDate(sprintStartDate), sprintName));
//        }

//        Date entryDate = (issueCreationDate.before(sprintStartDate)) ? sprintStartDate : issueCreationDate;
//        List<Document> statusesHistoryData = MongoUtils.getIssueStatusesDataFromIssueDoc(issueFromDB);
//        if (statusHistoryChanges.isEmpty() && statusesHistoryData.isEmpty()) {
//            statusHistoryChanges.add(new StatusHistoryChange(entryDate, DateUtils
//                    .getExecutionDate(entryDate), status));
//        }

//        List<Document> assigneesHistoryData = MongoUtils.getIssueAssigneesDataFromIssueDoc(issueFromDB);
//        if (assigneeHistoryChanges.isEmpty() && assigneesHistoryData.isEmpty()) {
//            assigneeHistoryChanges.add(new AssigneeHistoryChange(entryDate, DateUtils
//                    .getExecutionDate(entryDate), assignee));
//        }
    }

    private String handleAssigneeHistoryChanges(LinkedList<AssigneeHistoryChange> assigneeHistoryChanges, Document
            historyDoc, Date historyLastUpdateDate) {
        Pair<String, String> assigneeFromToPair = MongoUtils.getAssigneeChangedFromHistoryDoc(historyDoc);
        String currentAssigneeFrom = null;
        if (assigneeFromToPair != null) {
            currentAssigneeFrom = assigneeFromToPair.getValue1();
            if (currentAssigneeFrom.isEmpty()) {
                currentAssigneeFrom = "Unassigned";
            }

            String currentAssigneeTo = assigneeFromToPair.getValue2();
            if (currentAssigneeTo.isEmpty()) {
                currentAssigneeTo = "Unassigned";
            }
            boolean assigneeChanged = !currentAssigneeFrom.equals(currentAssigneeTo);
            if (assigneeChanged) {
                assigneeHistoryChanges.add(new AssigneeHistoryChange(historyLastUpdateDate, DateUtils
                        .getExecutionDate(historyLastUpdateDate), currentAssigneeTo));
            }
        }
        return currentAssigneeFrom;
    }

    private String handleStatusHistoryChanges(LinkedList<StatusHistoryChange> statusHistoryChanges, Document
            historyDoc, Date historyLastUpdateDate) {
        Pair<String, String> statusFromToPair = MongoUtils.getStatusChangedFromHistoryDoc(historyDoc);
        boolean statusChanged = false;
        String currentStatusFrom = null;
        if (statusFromToPair != null) {
            currentStatusFrom = statusFromToPair.getValue1();
            String currentStatusTo = statusFromToPair.getValue2();
            statusChanged = !currentStatusFrom.equals(currentStatusTo);
            if (statusChanged) {
                statusHistoryChanges.add(new StatusHistoryChange(historyLastUpdateDate, DateUtils
                        .getExecutionDate(historyLastUpdateDate), currentStatusTo));
            }
        }
        return currentStatusFrom;
    }

    private String handleSprintHistoryChanges(LinkedList<SprintHistoryChange> sprintsHistoryChanges, Document
            historyDoc, Date historyLastUpdateDate) {
        Pair<String, String> sprintFromToPair = MongoUtils.getSprintNameChangedFromHistoryDoc(historyDoc);
        boolean sprintChanged = false;
        String currentSprintNameFrom = null;
        if (sprintFromToPair != null) {
            currentSprintNameFrom = sprintFromToPair.getValue1();
            String currentSprintNameTo = sprintFromToPair.getValue2();
            sprintChanged = !currentSprintNameFrom.equals(currentSprintNameTo);
            if (sprintChanged) {
                sprintsHistoryChanges.add(new SprintHistoryChange(historyLastUpdateDate, DateUtils
                        .getExecutionDate(historyLastUpdateDate), currentSprintNameTo));
            }
        }
        return currentSprintNameFrom;
    }

    private Document updateExitingIssueDocument(String configID, String sprintName, String assignee, String status,
                                                String key, Date lastUpdateDate, String executionDate) {
        Document issueFromDB;
        issueFromDB = getIssueFromDBByKey(configID, key);
        issueFromDB.put(MongoUtils.ISSUE_DOC_KEY_STATUS, status);
        issueFromDB.put(MongoUtils.ISSUE_DOC_KEY_ASSIGNEE, assignee);
        issueFromDB.put(MongoUtils.DOCUMENT_KEY_SPRINT_NAME, sprintName);
        issueFromDB.put(MongoUtils.ISSUE_DOC_KEY_EXECUTION_DATE, executionDate);
        issueFromDB.put(MongoUtils.ISSUE_DOC_KEY_LAST_UPDATE, DateUtils.getDateInUTC(lastUpdateDate));
        String id = MongoUtils.getIDFromDoc(issueFromDB);
        MongoConnectionManager.updateDocument(MongoDBCollection.ISSUES.name(), issueFromDB, id);
        return issueFromDB;
    }

    private Document getIssueFromDBByKey(String configID, String key) {
        BasicDBObject filter = new BasicDBObject();
        filter.append(MongoUtils.DOCUMENT_KEY_CONFIGֹID, configID);
        filter.append(MongoUtils.ISSUE_DOC_KEY_KEY, key);

        List<Document> issues = MongoConnectionManager
                .findDocumentsInCollectionByFilter(MongoDBCollection.ISSUES.name(), filter);
        return issues.isEmpty() ? null : issues.iterator().next();
    }

    private List<Document> getIssuesFromDBBySprintName(String configID, String sprintName) {
        BasicDBObject filter = new BasicDBObject();
        filter.append(MongoUtils.DOCUMENT_KEY_CONFIGֹID, configID);
        filter.append(MongoUtils.DOCUMENT_KEY_SPRINT_NAME, sprintName);
        List<Document> issues = MongoConnectionManager
                .findDocumentsInCollectionByFilter(MongoDBCollection.ISSUES.name(), filter);
        return issues.isEmpty() ? null : issues;
    }

    private static List<Document> getHistoryDocsForDate(Document issueDoc, Date forDate) {
        return MongoUtils.getHistoryDocsFromIssueDocForDateSorted(issueDoc, forDate, false);
    }

    private static Date[] getDatesForSprintHistory(Document sprintDoc) {
        Document sprintInfoDoc = MongoUtils.getSprintInfoDocFromSprintDoc(sprintDoc);
        Date sprintStartDate = MongoUtils.getSprintStartDateFromSprintDoc(sprintDoc);
        Date sprintEndDate = MongoUtils.getSprintEndDateFromSprintDoc(sprintDoc);
        String sprintState = MongoUtils.getSprintStateFromSprintMetaDataDoc(sprintInfoDoc);
        List<Date> res;

        if ("closed".equalsIgnoreCase(sprintState)) {
            res = DateUtils.getAllDatesBetweenDates(DateUtils.getSameDateInMidnight(sprintStartDate), sprintEndDate,
                    false);
        } else {
            Date today = new Date();
            res = DateUtils.getAllDatesBetweenDates(DateUtils.getSameDateInMidnight(sprintStartDate), today, false);
        }

        return res.toArray(new Date[]{});
    }

    private void createIssueDocument(String configID, String sprintName, String projectName, String key, String url,
                                     String status, String assignee, Date lastUpdateDate, String executionDate) {

        Document newIssueDoc = new Document();

        newIssueDoc.append(MongoUtils.DOCUMENT_KEY_CONFIGֹID, configID);
        newIssueDoc.append(MongoUtils.DOCUMENT_KEY_PROJECT_NAME, projectName);
        newIssueDoc.append(MongoUtils.ISSUE_DOC_KEY_KEY, key);
        newIssueDoc.append(MongoUtils.DOCUMENT_KEY_SPRINT_NAME, sprintName);
        newIssueDoc.append(MongoUtils.ISSUE_DOC_KEY_STATUS, status);
        newIssueDoc.append(MongoUtils.ISSUE_DOC_KEY_ASSIGNEE, assignee);
        newIssueDoc.append(MongoUtils.ISSUE_DOC_KEY_URL, url);
        newIssueDoc.append(MongoUtils.ISSUE_DOC_KEY_LAST_UPDATE, DateUtils.getDateInUTC(lastUpdateDate));
        newIssueDoc.append(MongoUtils.ISSUE_DOC_KEY_EXECUTION_DATE, executionDate);

        //history
        List<Document> emptyDataArray = new ArrayList<>();
        newIssueDoc.append(MongoUtils.ISSUE_DOC_KEY_SPRINTS_DATA, emptyDataArray);
        newIssueDoc.append(MongoUtils.ISSUE_DOC_KEY_STATUSES_DATA, emptyDataArray);
        newIssueDoc.append(MongoUtils.ISSUE_DOC_KEY_ASSIGNEES_DATA, emptyDataArray);

        MongoConnectionManager.addDocument(MongoDBCollection.ISSUES.name(), newIssueDoc);
        tracer.trace("new issue created in db: " + key);
    }

    private void addSprintHistoryDataToList(SprintHistoryChange sprintHistoryChange, List
            <Document> sprintsData) {
        Document sprintEntryDoc = new Document();
        sprintEntryDoc.append(MongoUtils.SPRINT_HISTORY_DOC_KEY_SPRINT_NAME, sprintHistoryChange.getSprintName());
        sprintEntryDoc.append(MongoUtils.SPRINT_HISTORY_DOC_KEY_ADDED_AT, DateUtils.getDateInUTC(sprintHistoryChange
                .getChangedAt()));
        sprintEntryDoc.append(MongoUtils.SPRINT_HISTORY_DOC_KEY_ADD_DATE, sprintHistoryChange.getChangedDate());

        if (!sprintsData.contains(sprintEntryDoc)) {
            sprintsData.add(sprintEntryDoc);
        }
    }

    private void addStatusHistoryDataToIssue(StatusHistoryChange statusHistoryChange, List<Document> statusesData) {
        Document statusEntryDoc = new Document();
        statusEntryDoc.append(MongoUtils.STATUS_HISTORY_DOC_KEY_STATUS, statusHistoryChange.getStatus());
        statusEntryDoc.append(MongoUtils.STATUS_HISTORY_DOC_KEY_CHANGED_AT, DateUtils.getDateInUTC(statusHistoryChange
                .getChangedAt()));
        statusEntryDoc.append(MongoUtils.STATUS_HISTORY_DOC_KEY_CHANGED_DATE, statusHistoryChange.getChangedDate());

        if (!statusesData.contains(statusEntryDoc)) {
            statusesData.add(statusEntryDoc);
        }
    }

    private void addAssigneeHistoryDataToIssue(AssigneeHistoryChange assigneeHistoryChange, List<Document>
            assigneesData) {

        Document assigneeEntryDoc = new Document();
        assigneeEntryDoc.append(MongoUtils.ASSIGNEE_HISTORY_DOC_KEY_ASSIGNEE, assigneeHistoryChange.getAssignee());
        assigneeEntryDoc.append(MongoUtils.ASSIGNEE_HISTORY_DOC_KEY_ASSIGNED_AT, DateUtils.getDateInUTC
                (assigneeHistoryChange.getChangedAt()));
        assigneeEntryDoc.append(MongoUtils.ASSIGNEE_HISTORY_DOC_KEY_ASSIGN_DATE, assigneeHistoryChange.getChangedDate
                ());

        if (!assigneesData.contains(assigneeEntryDoc)) {
            assigneesData.add(assigneeEntryDoc);
        }

    }

    private void addCommitDocument(String configID, String sprintName, String projectName, String executor, String url,
                                   String lastUpdate, String status, String key, ArrayList<String> relatedIssues, int
                                           buildNumber) {
        Document issueDoc = new Document();

        Date lastUpdateDate = DateUtils.formatDateStringToDate(lastUpdate);
        lastUpdate = lastUpdateDate.toString();
        java.sql.Date executionDate = new java.sql.Date(lastUpdateDate.getYear(), lastUpdateDate.getMonth(),
                lastUpdateDate.getDate());
        String dateString = executionDate.toString();

        issueDoc.append(MongoUtils.DOCUMENT_KEY_CONFIGֹID, configID);
        issueDoc.append(MongoUtils.COMMIT_DOC_KEY_ISSUE_KEY, relatedIssues);
        issueDoc.append(MongoUtils.COMMIT_DOC_KEY_BUILD_NUMBER, Integer.valueOf(buildNumber));
        issueDoc.append("sprintName", sprintName);
        issueDoc.append("projectName", projectName);
        issueDoc.append("executor", executor);
        issueDoc.append("url", url);
        issueDoc.append("lastUpdate", DateUtils.getDateInUTC(lastUpdateDate));
        issueDoc.append("status", status);
        issueDoc.append("description", key);
        issueDoc.append("executionDate", dateString);

        MongoConnectionManager.addDocument(MongoDBCollection.COMMITS.name(), issueDoc);
    }

    public static SimpleDateFormat getDateFormat(String date) {
        SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy");
        Date res = null;
        try {
            res = f.parse(date);
        } catch (ParseException e) {
            // do nothing
        }
        if (res != null)
            return f;

        f = new SimpleDateFormat("dd-MMM-yy hh:mm aaa");
        f.setDateFormatSymbols(DateFormatSymbols.getInstance(Locale.ENGLISH));
        try {
            res = f.parse(date);
        } catch (ParseException e) {
            // do nothing
        }
        if (res != null)
            return f;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        SimpleDateFormat output = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date d = sdf.parse(date);
            String formattedTime = output.format(d);
            res = output.parse(formattedTime);
        } catch (ParseException e) {
            // do nothing
        }

        if (res != null)
            return f;

        try {
            f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            Date d = sdf.parse(date);
            String formattedTime = output.format(d);
            res = output.parse(formattedTime);
        } catch (ParseException e) {
            // do nothing
        }

        if (res != null)
            return f;

        try {
            f = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
            res = f.parse(date);
        } catch (ParseException e) {
            // do nothing
        }

        if (res != null)
            return f;

        return null;
    }

    private void computeAlerts(String projectName, String sprintName) {
        alertsEngine.computeAndTriggerAlerts(projectName, sprintName);
    }


    private abstract static class HistoryChange {
        private Date changedAt;
        private String changedDate;

        public HistoryChange(Date changedAt, String changedDate) {
            this.changedAt = changedAt;
            this.changedDate = changedDate;
        }

        public Date getChangedAt() {
            return changedAt;
        }

        public String getChangedDate() {
            return changedDate;
        }
    }

    private static class StatusHistoryChange extends HistoryChange {
        private String status;

        public StatusHistoryChange(Date changedAt, String changedDate, String status) {
            super(changedAt, changedDate);
            this.status = status;
        }

        public String getStatus() {
            return status;
        }
    }

    private static class SprintHistoryChange extends HistoryChange {
        private String sprintName;

        public SprintHistoryChange(Date changedAt, String changedDate, String sprintName) {
            super(changedAt, changedDate);
            this.sprintName = sprintName;
        }

        public String getSprintName() {
            return sprintName;
        }
    }

    private static class AssigneeHistoryChange extends HistoryChange {
        private String assignee;

        public AssigneeHistoryChange(Date changedAt, String changedDate, String assignee) {
            super(changedAt, changedDate);
            this.assignee = assignee;
        }

        public String getAssignee() {
            return assignee;
        }
    }

    private static class DocumentDateComparator implements Comparator<Document> {
        private String dateFieldToSort;

        public DocumentDateComparator(String dateFieldToSort) {
            this.dateFieldToSort = dateFieldToSort;
        }

        @Override
        public int compare(Document o1, Document o2) {
            Date o1Date = o1.getDate(dateFieldToSort);
            Date o2Date = o2.getDate(dateFieldToSort);

            if (o1.equals(o2Date)) {
                return 0;
            }
            if (o1Date.before(o2Date)) {
                return 1;
            }
            return -1;
        }
    }
}

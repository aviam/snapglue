package com.nigealm.gadgets.svc;

import com.mongodb.client.FindIterable;
import com.nigealm.mongodb.MongoUtils;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Consumes({"application/json"})
@Produces({"application/json"})

@Path("/trend")
public class SprintTrendGadgetServiceImpl implements SprintTrendGadgetService {

    private static final String NUMBER_OF_COMMITS_HEADER = "NumberOfCommits";
    private static final String NUMBER_OF_OPEN_ISSUES_HEADER = "NumberOfOpenIssues";
    private static final String NUMBER_OF_CLOSED_ISSUES_HEADER = "NumberOfClosedIssues";
    private static final String NUMBER_OF_SUCCESSFUL_BUILDS_HEADER = "NumberOfSuccessfulBuilds";
    private static final String NUMBER_OF_FAILED_BUILDS_HEADER = "NumberOfFailedBuilds";
    private static final String LABELS_CHANGES_TREND_HEADER = "labelsChangesTrend";
    private static final String SERIES_CHANGES_TREND_HEADER = "seriesChangesTrend";
    private static final String DATA_CHANGES_TREND_HEADER = "dataChangesTrend";

    @Override
    @GET
    @Path("/getSprintsTrend")
    @PreAuthorize("hasRole('ROLE_USER')")
    public String getSprintsData() {


        return getData(false);
    }

    @Override
    @GET
    @Path("/getExternalTrend")
    @PreAuthorize("hasRole('ROLE_USER')")
    public String getExternalData() {
        return getData(true);
    }

    private String getData(boolean isExternalData) {
        try {
            List<String> allActiveSprints;
            JSONArray sprintsTrendDataArray = new JSONArray();

            if (isExternalData) {
                allActiveSprints = new LinkedList<>();
                allActiveSprints.add("External Data");
            } else {
                allActiveSprints = MongoUtils.getActiveSprintNames();
            }

            for (String sprintName : allActiveSprints) {
                Map<Date, SprintTrendDayRecord> trendMap = new TreeMap<>(new DateComparator());
                Map<Date, List<Document>> dateToBuildsMap = new HashMap<>();
                Map<Date, List<Document>> dateToCommitsMap = new HashMap<>();
                Map<Date, Set<Document>> dateToIssuesMap = new HashMap<>();
                List<Date> datesList = buildDatesList(sprintName);

                buildDateToDataMaps(sprintName, dateToBuildsMap, dateToCommitsMap);
                calculateTrendData(sprintName, datesList, trendMap, dateToBuildsMap, dateToCommitsMap, dateToIssuesMap);
                JSONObject currSprintTrendData = buildSprintTrendObject(sprintName, trendMap);
                sprintsTrendDataArray.put(currSprintTrendData);
            }

            return sprintsTrendDataArray.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "[]";
        }
    }

    private List<Date> buildDatesList(String sprintName) {
        List<Date> datesList = new LinkedList<>();
        Date startDate = MongoUtils.getSprintStartDateFromSprintName(sprintName);
        Date endDate = MongoUtils.getSprintEndDateFromSprintName(sprintName);
        Date currDate = Calendar.getInstance().getTime();

        if (currDate.before(endDate)) {
            endDate = currDate;
        }

        Calendar currCal = Calendar.getInstance();
        currCal.setTime(removeTimeFromDate(startDate));

        while (currCal.getTime().before(endDate)) {
            datesList.add(currCal.getTime());
            currCal.add(Calendar.DAY_OF_MONTH, 1);
        }

        datesList.add(endDate);
        Collections.sort(datesList, new DateComparator());
        return datesList;
    }

    private void calculateTrendData(String sprintName, List<Date> datesList, Map<Date, SprintTrendDayRecord> trendMap,
                                    Map<Date, List<Document>> dateToBuildsMap, Map<Date, List<Document>>
                                            dateToCommitsMap, Map<Date, Set<Document>>
                                            dateToIssuesMap) {
        for (Date currDate : datesList) {
            SprintTrendDayRecord currDayRecord = new SprintTrendDayRecord();
            calculateNumberOfCommits(currDate, dateToCommitsMap, currDayRecord);
            calculateNumberOfSuccessfulBuilds(currDate, dateToBuildsMap, currDayRecord);
            calculateNumberOfFailedBuilds(currDate, dateToBuildsMap, currDayRecord);
            trendMap.put(currDate, currDayRecord);
        }

        buildDateToIssuesMap(sprintName, datesList, dateToIssuesMap);
        calculateOpenAndClosedIssues(datesList, dateToIssuesMap, trendMap);
    }

    private void buildDateToIssuesMap(String sprintName, List<Date> datesList, Map<Date, Set<Document>>
            dateToIssuesMap) {
        for (Date date : datesList){
            dateToIssuesMap.put(date, new HashSet<Document>());
        }

        FindIterable<Document> dataDocuments;

        if ("External Data".equals(sprintName)) {
            dataDocuments = MongoUtils.getAllExternalDataDocs();
        } else {
            dataDocuments = MongoUtils.getAllSprintDocs(sprintName);
        }

        for (Document currSprintDoc : dataDocuments) {
            Document jiraDoc = MongoUtils.getJiraDocFromSprintDoc(currSprintDoc);
            if (jiraDoc == null) {
                continue;
            }

            List<Document> completedSprintIssues = MongoUtils.getJiraDocsCompletedIssues(jiraDoc);
            List<Document> openSprintIssues = MongoUtils.getJiraDocsIncompletedIssues(jiraDoc);

            for (Document currIssueDoc : completedSprintIssues) {
                addJiraDocToMap(datesList, dateToIssuesMap, currIssueDoc);
            }

            for (Document currIssueDoc : openSprintIssues) {
                addJiraDocToMap(datesList, dateToIssuesMap, currIssueDoc);
            }
        }


    }

    private void addJiraDocToMap(List<Date> datesList, Map<Date, Set<Document>> dateToIssuesMap, Document currIssueDoc) {
        Date createdDate = MongoUtils.getIssueCreatedDateFromIssueDoc(currIssueDoc);
        createdDate = removeTimeFromDate(createdDate);

        if (createdDate == null){
            return;
        }

        if (createdDate.before(datesList.get(0))){
            Set<Document> issuesList = dateToIssuesMap.get(datesList.get(0));
            issuesList.add(currIssueDoc);
            return;
        }

        for (Date currDate : datesList) {
            if (createdDate.after(currDate) || createdDate.equals(currDate)) {
                Set<Document> issuesList = dateToIssuesMap.get(createdDate);
                if (issuesList == null) {
                    continue;
                }
                issuesList.add(currIssueDoc);
            }
        }
    }

    private void calculateOpenAndClosedIssues(List<Date> datesList, Map<Date, Set<Document>> dateToIssuesMap, Map<Date,
            SprintTrendDayRecord> trendMap) {
        Set<String> openIssuesIdSet = new HashSet<>();
        Set<String> closedIssuesIdSet = new HashSet<>();
        for (Date currDate : datesList) {
            for (Document jiraDoc : dateToIssuesMap.get(currDate)) {
                updateIssuesLists(currDate, openIssuesIdSet, closedIssuesIdSet, jiraDoc);
            }
            updateDayRecord(trendMap, openIssuesIdSet, closedIssuesIdSet, currDate);
        }
    }

    private void updateIssuesLists(Date currDate, Set<String> openIssuesIdSet, Set<String> closedIssuesIdSet, Document currIssueDoc) {
        String status = MongoUtils.getIssueStatusFromIssueDoc(currIssueDoc);
        String issueId = currIssueDoc.getString("key");
        Date lastUpdatedDate = MongoUtils.getIssueUpdatedDateFromIssueDoc(currIssueDoc);
        lastUpdatedDate = removeTimeFromDate(lastUpdatedDate);
        if (lastUpdatedDate == null){
            return;
        }
        if ("done".equalsIgnoreCase(status) && (currDate.equals(lastUpdatedDate) || currDate.after(lastUpdatedDate))){
            openIssuesIdSet.remove(issueId);
            closedIssuesIdSet.add(issueId);
        } else{
            closedIssuesIdSet.remove(issueId);
            openIssuesIdSet.add(issueId);
        }
    }

    private void updateDayRecord(Map<Date, SprintTrendDayRecord> trendMap, Set<String> openIssuesIdSet, Set<String>
            closedIssuesIdSet, Date currDate) {
        SprintTrendDayRecord currDayRecord = trendMap.get(currDate);
        if (currDayRecord == null) {
            currDayRecord = new SprintTrendDayRecord();
            trendMap.put(currDate, currDayRecord);
        }
        currDayRecord.setNumberOfOpenIssues(openIssuesIdSet.size());
        currDayRecord.setNumberOfClosedIssues(closedIssuesIdSet.size());
    }

    private JSONObject buildSprintTrendObject(String sprintName, Map<Date, SprintTrendDayRecord> trendMap) throws
            JSONException {
        JSONObject sprintTrendData = new JSONObject();
        sprintTrendData.put("sprint", sprintName);
        buildLabelsHeader(sprintTrendData, trendMap);
        buildSeriesHeader(sprintTrendData);
        buildDataObject(sprintTrendData, trendMap);
        return sprintTrendData;
    }

    private void buildDataObject(JSONObject sprintTrendData, Map<Date, SprintTrendDayRecord> trendMap) throws
            JSONException {
        JSONArray allDaysDataArray = new JSONArray();
        for (SprintTrendDayRecord sprintTrendDayRecord : trendMap.values()) {
            JSONArray currDayDataArray = new JSONArray();

            currDayDataArray.put(sprintTrendDayRecord.getNumberOfCommits());
            currDayDataArray.put(sprintTrendDayRecord.getNumberOfSuccessfulBuilds());
            currDayDataArray.put(sprintTrendDayRecord.getNumberOfFailedBuilds());
            currDayDataArray.put(sprintTrendDayRecord.getNumberOfOpenIssues());
            currDayDataArray.put(sprintTrendDayRecord.getNumberOfClosedIssues());

            allDaysDataArray.put(currDayDataArray);
        }

        sprintTrendData.put(DATA_CHANGES_TREND_HEADER, allDaysDataArray);
    }

    private void buildSeriesHeader(JSONObject sprintTrendData) throws JSONException {
        JSONArray seriesLabelsArray = new JSONArray();

        seriesLabelsArray.put(NUMBER_OF_COMMITS_HEADER);
        seriesLabelsArray.put(NUMBER_OF_SUCCESSFUL_BUILDS_HEADER);
        seriesLabelsArray.put(NUMBER_OF_FAILED_BUILDS_HEADER);
        seriesLabelsArray.put(NUMBER_OF_OPEN_ISSUES_HEADER);
        seriesLabelsArray.put(NUMBER_OF_CLOSED_ISSUES_HEADER);

        sprintTrendData.put(SERIES_CHANGES_TREND_HEADER, seriesLabelsArray);
    }

    private void buildLabelsHeader(JSONObject sprintTrendData, Map<Date, SprintTrendDayRecord> trendMap) throws
            JSONException {
        Set<Date> dates = trendMap.keySet();
        JSONArray datesArray = new JSONArray();

        for (Date date : dates) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            String currDate = "" + calendar.get(Calendar.DAY_OF_MONTH);
            currDate += "/" + (calendar.get(Calendar.MONTH) + 1);
            currDate += "/" + calendar.get(Calendar.YEAR);
            datesArray.put(currDate);
        }

        sprintTrendData.put(LABELS_CHANGES_TREND_HEADER, datesArray);
    }

    private void calculateNumberOfFailedBuilds(Date date, Map<Date, List<Document>> dateToBuildsMap,
                                               SprintTrendDayRecord currDayRecord) {
        int numOfSuccessfulBuilds = getNumberOfBuildByStatusByDate(date, dateToBuildsMap, "FAILURE");
        currDayRecord.setNumberOfFailedBuilds(numOfSuccessfulBuilds);
    }

    private void calculateNumberOfSuccessfulBuilds(Date date, Map<Date, List<Document>> dateToBuildsMap,
                                                   SprintTrendDayRecord currDayRecord) {
        int numOfSuccessfulBuilds = getNumberOfBuildByStatusByDate(date, dateToBuildsMap, "SUCCESS");
        currDayRecord.setNumberOfSuccessfulBuilds(numOfSuccessfulBuilds);
    }

    private int getNumberOfBuildByStatusByDate(Date date, Map<Date, List<Document>> dateToBuildsMap, String status) {
        Set<Integer> requiredBuildsSet = new HashSet<>();

        List<Document> allSprintBuilds = dateToBuildsMap.get(date);
        if (allSprintBuilds== null){
            allSprintBuilds = new LinkedList<>();
        }
        for (Document buildDoc : allSprintBuilds) {
            int buildNumber = buildDoc.getInteger("number");
            String buildResult = buildDoc.getString("result");
            if (status.equalsIgnoreCase(buildResult)) {
                requiredBuildsSet.add(buildNumber);
            }
        }

        return requiredBuildsSet.size();
    }

    private void calculateNumberOfCommits(Date date, Map<Date, List<Document>>
            dateToCommitsMap, SprintTrendDayRecord currDayRecord) {
        Set<String> commitIdsSet = new HashSet<>();
        List<Document> allSprintCommits = dateToCommitsMap.get(date);
        if (allSprintCommits== null){
            allSprintCommits = new LinkedList<>();
        }
        for (Document commitDoc : allSprintCommits) {
            String commitId = commitDoc.getString("id");
            commitIdsSet.add(commitId);
        }
        currDayRecord.setNumberOfCommits(commitIdsSet.size());
    }

    private void buildDateToDataMaps(String sprintName, Map<Date, List<Document>> dateToBuildsMap, Map<Date,
            List<Document>> dateToCommitsMap) {

        FindIterable<Document> dataDocuments;

        if ("External Data".equals(sprintName)) {
            dataDocuments = MongoUtils.getAllExternalDataDocs();
        } else {
            dataDocuments = MongoUtils.getAllSprintDocs(sprintName);
        }

        for (Document currSprintDoc : dataDocuments) {
            buildDateToCommitsMap(currSprintDoc, dateToCommitsMap);
            buildDateToBuildsMap(currSprintDoc, dateToBuildsMap);
        }
    }


    private void buildDateToBuildsMap(Document currSprintDoc, Map<Date, List<Document>> dateToBuildsMap) {
        List<Document> allBuildsDocs = MongoUtils.getAllJenkinsDocsFromSprintDoc(currSprintDoc);
        for (Document currBuild : allBuildsDocs) {
            Date buildDate = removeTimeFromDate(MongoUtils.getBuildExecutionTimeFromBuildDoc(currBuild));
            if (buildDate == null) {
                continue;
            }

            List<Document> dateBuilds = dateToBuildsMap.get(buildDate);
            if (dateBuilds == null) {
                dateBuilds = new LinkedList<>();
                dateToBuildsMap.put(buildDate, dateBuilds);
            }

            dateBuilds.add(currBuild);
        }
    }

    private Date removeTimeFromDate(Date date) {
        if (date == null){
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private void buildDateToCommitsMap(Document currSprintDoc, Map<Date, List<Document>> dateToCommitsMap) {
        List<Document> allCommitDocs = MongoUtils.getAllCommitDocsFromSprintDoc(currSprintDoc);
        for (Document currCommit : allCommitDocs) {
            Date commitDate = removeTimeFromDate(MongoUtils.getCommitExecutionTimeFromCommitDoc(currCommit));
            if (commitDate == null) {
                continue;
            }
            List<Document> dateCommits = dateToCommitsMap.get(commitDate);
            if (dateCommits == null) {
                dateCommits = new LinkedList<>();
                dateToCommitsMap.put(commitDate, dateCommits);
            }
            dateCommits.add(currCommit);
        }
    }

    private class DateComparator implements Comparator<Date> {
        @Override
        public int compare(Date o1, Date o2) {
            if (o1 == null) {
                return -1;
            }
            if (o2 == null) {
                return 1;
            }
            return o1.compareTo(o2);
        }
    }
}

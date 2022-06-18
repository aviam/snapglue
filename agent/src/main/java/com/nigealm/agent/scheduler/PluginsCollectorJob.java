package com.nigealm.agent.scheduler;

import com.mongodb.client.FindIterable;
import com.nigealm.agent.impl.AgentDataCollectionException;
import com.nigealm.agent.impl.DataPerSprint;
import com.nigealm.agent.impl.bamboo.BambooRetrieverExecutor;
import com.nigealm.agent.impl.bitbucket.BitbucketRetrieverExecutor;
import com.nigealm.agent.impl.github.GitHubRetrieverExecutor;
import com.nigealm.agent.impl.gitlab.GitLabRetrieverExecutor;
import com.nigealm.agent.impl.jenkins.JenkinsRetrieverExecutor;
import com.nigealm.agent.impl.jira.JiraIssuesRetriever;
import com.nigealm.agent.svc.MongoDBAgentServiceImpl;
import com.nigealm.agent.users.UserManager;
import com.nigealm.common.utils.Tracer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.MDC;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.quartz.*;

import java.util.*;
import java.util.Calendar;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.nigealm.agent.impl.MongoDBConstants.*;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class PluginsCollectorJob implements Job, InterruptableJob, Runnable {
    private final static Tracer tracer = new Tracer(PluginsCollectorJob.class);
    private final static ConcurrentLinkedQueue<String> confCurrentlyActive = new ConcurrentLinkedQueue<>();
    private static final String USER = "snapglueg@gmail.com";
    private static final String PASSWORD = "snapgluethebest";
    private static final String SERVICE_NAME = "pluginsService";
    private static String host;
    private static String port;
    private static String serverPath;
    private AtomicReference<Thread> runningThread = new AtomicReference<>();
    private AtomicBoolean stopFlag = new AtomicBoolean(false);
    private boolean retrieveAllData;
    private JSONObject mergedData = null;
    private UserManager userManager = null;
    private String confId;
    private Map<String, Set<String>> jiraToSprintMap;
    private Map<String, Set<String>> gitlabCommitToSprintMap;
    private Map<String, Set<String>> githubCommitToSprintMap;
    private Map<String, Set<String>> bitbucketCommitToSprintMap;
    private Date lastExecutionTime;
    private Map<String,Pair<Date,Date>> sprintsDatesMap;

    public PluginsCollectorJob() {
        runningThread.set(Thread.currentThread());
        mergedData = new JSONObject();
        userManager = new UserManager();
        jiraToSprintMap = new ConcurrentSkipListMap<>();
        gitlabCommitToSprintMap = new ConcurrentHashMap<>();
        githubCommitToSprintMap = new ConcurrentHashMap<>();
        bitbucketCommitToSprintMap = new ConcurrentHashMap<>();
    }

    public PluginsCollectorJob(JobDetail jobDetail) {
        this();
        init(jobDetail);
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        boolean shouldRemoveConfigurationFromQueue = true;
        try {
            init(context.getJobDetail());
            MDC.put("config_id", confId);
            tracer.entry("execute");
            if (confCurrentlyActive.contains(confId)) {
                shouldRemoveConfigurationFromQueue = false;
                tracer.warn("There is already active collector for conf: " + confId);
                return;
            }
            confCurrentlyActive.add(confId);
            tracer.info("Start execution for configuration: " + confId);
            CookieStore cookieStore = doLogin();

//			AgentLastExecutionData data = getLastExecutionDataForConfigurationID(confId, cookieStore);

            // get last Execution data of the agent
            AgentLastExecutionData data = getLastExecutionData(cookieStore, confId);
            lastExecutionTime = data.getLastExecutionTime();
            int lastBuildNumber = data.getLastBuildNumber();

            // check condition before retrieving data
            if (stopFlag.get()) return;
            // retrieve data
            tracer.info("Last execution Date is  " + lastExecutionTime + " , Last Build Number is " + lastBuildNumber);
            boolean dataCollectedSuccessfully;
            try {
                dataCollectedSuccessfully = iterateToolsAndRetrieveData(lastBuildNumber);
            } catch (AgentDataCollectionException e) {
                tracer.exception("execute", e);
                dataCollectedSuccessfully = false;
            }

            // check condition before saving to DB
            if (stopFlag.get()) return;

            if (dataCollectedSuccessfully) {
                // save data in db
                saveDataToDB(cookieStore);
            }

        } catch (Exception e) {
            tracer.exception("execute", e);
        } finally {
            if (shouldRemoveConfigurationFromQueue){
                confCurrentlyActive.remove(confId);
            }
            MDC.remove("config_id");
            runningThread.set(null);
            tracer.exit("execute");
        }
    }

    private void collectDataSync() {
        MDC.put("config_id", confId);
        tracer.entry("collect sync for confId: " + confId);
        try {
            confCurrentlyActive.add(confId);
            CookieStore cookieStore = doLogin();

            // get last Execution data of the agent
            AgentLastExecutionData data = getLastExecutionData(cookieStore, confId);
            lastExecutionTime = data.getLastExecutionTime();
            //Date lastExecutionTime=new Date(0);
            int lastBuildNumber = data.getLastBuildNumber();

            // check condition before retrieving data
            if (stopFlag.get()) return;
            // retrieve data
            boolean dataCollectedSuccessfully;
            try {
                dataCollectedSuccessfully = iterateToolsAndRetrieveData(lastBuildNumber);
            } catch (AgentDataCollectionException e) {
                tracer.exception("execute", e);
                dataCollectedSuccessfully = false;
            }

            // check condition before saving to DB
            if (stopFlag.get()) return;

            // save data in db
            if (dataCollectedSuccessfully) {
                // save data in db
                saveDataToDB(cookieStore);
            }

        } catch (Exception e) {
            tracer.exception("execute", e);
        } finally {
            runningThread.set(null);
            confCurrentlyActive.remove(confId);
            MDC.remove("config_id");
            tracer.exit("collectDataSync");
        }
    }

    private void init(JobDetail jobDetail) {
        confId = jobDetail.getJobDataMap().getString("confId");
        retrieveAllData = jobDetail.getJobDataMap().getBoolean("retrieveAll");
        host = jobDetail.getJobDataMap().getString("serverHost");
        port = jobDetail.getJobDataMap().getString("serverPort");
        serverPath = jobDetail.getJobDataMap().getString("serverPath");
    }

    private CookieStore doLogin() throws Exception {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        CookieStore res;

        String loginURL = getServerBaseURL() + "/j_spring_security_check";
        HttpPost loginRequest = new HttpPost(loginURL);

        // Prepare post parameters
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("j_username", USER));
        nvps.add(new BasicNameValuePair("j_password", PASSWORD));
        loginRequest.setEntity(new UrlEncodedFormEntity(nvps, Consts.UTF_8));

        // just make the login
        HttpResponse loginResponse = httpClient.execute(loginRequest);
        tracer.trace("loginResponse: " + loginResponse.getStatusLine().getStatusCode());

        res = httpClient.getCookieStore();

        return res;
    }

    private boolean iterateToolsAndRetrieveData(int lastBuildNumber) throws
            AgentDataCollectionException {
        try {
            buildMergedDataObject();
        } catch (JSONException e) {
            tracer.exception("iterateToolsAndRetrieveData", e);
            return false;
        }

        MongoDBAgentServiceImpl mongoService = new MongoDBAgentServiceImpl();
        FindIterable<org.bson.Document> allDocuments = mongoService.getAllDocuments();
        boolean configurationFound = false;
        for (org.bson.Document document : allDocuments) {
            String currentConfId = document.get("_id").toString();
            if (!confId.equals(currentConfId)) {
                continue;
            }

            configurationFound = true;
            org.bson.Document toolsDoc = (org.bson.Document) document.get("tools");
            tracer.info("start iterating JIRA...");
            iterateJira(toolsDoc); // jira must be the first tool been iterated!
            tracer.info("Done iterating JIRA");
            tracer.info("Start iterating SCM tool...");
            HashMap<String, JSONObject> commitIdToCommitObjectMap = new HashMap<>();
            iterateSCMTools(toolsDoc, commitIdToCommitObjectMap);
            tracer.info("Done iterating SCM tool");
            tracer.info("Start iterating Builds tool...");
            iterateBuildTools(toolsDoc, lastBuildNumber, commitIdToCommitObjectMap);
            tracer.info("Done iterating Builds tool");
            addUsersToMergedData();
        }

        if (!configurationFound) {
            tracer.warn("Configuration not found in db for configuration id: " + confId);
            return false;
        }

        return true;
    }

    private void addUsersToMergedData() throws AgentDataCollectionException {
        JSONArray usersArray = userManager.getUsersListAsJsonArray();
        try {
            mergedData.put("users", usersArray);
        } catch (JSONException e) {
            throw new AgentDataCollectionException(confId, "Users", e);
        }
    }

    private void iterateBuildTools(Document toolsDoc, int lastBuildNumber, HashMap<String, JSONObject>
            commitIdToCommitObjectMap) throws
            AgentDataCollectionException {
        if (toolsDoc == null) {
            tracer.warn("iterateBuildTools: Tools document is null");
            return;
        }

        ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        try {
            // Submit Jenkins Task
            Future<DataPerSprint> jenkinsFuture = executorService.submit(new JenkinsRetrieverExecutor(toolsDoc,
                    retrieveAllData, lastBuildNumber, sprintsDatesMap, userManager, githubCommitToSprintMap,
                    gitlabCommitToSprintMap, bitbucketCommitToSprintMap, commitIdToCommitObjectMap));
            // Submit Bamboo Task
            Future<DataPerSprint> bambooFuture = executorService.submit(new BambooRetrieverExecutor(toolsDoc,
                    retrieveAllData, lastExecutionTime, sprintsDatesMap, userManager, githubCommitToSprintMap,
                    gitlabCommitToSprintMap, bitbucketCommitToSprintMap));

            try {
                // Add Jenkins Data
                DataPerSprint jenkinsData = jenkinsFuture.get();
                if (jenkinsData != null)
                    addDataToMergedJsonObject("jenkins", jenkinsData);

                // Add Bamboo Data
                DataPerSprint bambooScmData = bambooFuture.get();
                if (bambooScmData != null)
                    addDataToMergedJsonObject("bamboo", bambooScmData);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                tracer.exception("iterateBuildTools", e);
            }
        } catch (Exception e) {
            throw new AgentDataCollectionException(confId, "Build", e);
        } finally {
            executorService.shutdown();
        }
    }

    private void iterateSCMTools(Document toolsDoc, HashMap<String, JSONObject> commitIdToCommitObjectMap) throws AgentDataCollectionException {
        if (toolsDoc == null) {
            tracer.warn("iterateSCMTools: Tools document is null");
            return;
        }
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        try {
            // Submit GitLab Task
            Future<DataPerSprint> gitlabFuture = executorService.submit(new GitLabRetrieverExecutor(confId, toolsDoc,
                    lastExecutionTime, sprintsDatesMap, gitlabCommitToSprintMap, jiraToSprintMap, userManager));
            // Submit GitHub Task
            Future<DataPerSprint> githubFuture = executorService.submit(new GitHubRetrieverExecutor(toolsDoc,
                    lastExecutionTime,sprintsDatesMap,
                    githubCommitToSprintMap, jiraToSprintMap, userManager));
            // Submit Bitbucket Task
            Future<DataPerSprint> bitbucketFuture = executorService.submit(new BitbucketRetrieverExecutor(confId,
                    toolsDoc, lastExecutionTime, sprintsDatesMap, bitbucketCommitToSprintMap, jiraToSprintMap,
                    userManager));
            try {
                // Add GitLab Data
                DataPerSprint gitLabScmData = gitlabFuture.get();
                if (gitLabScmData != null) {
                    addDataToMergedJsonObject("gitlab", gitLabScmData);
                    addCommitsToCommitsMap(gitLabScmData, commitIdToCommitObjectMap);
                }

                // Add GitHub Data
                DataPerSprint gitHubScmData = githubFuture.get();
                if (gitHubScmData != null) {
                    addDataToMergedJsonObject("github", gitHubScmData);
                    addCommitsToCommitsMap(gitHubScmData, commitIdToCommitObjectMap);
                }

                // Add Bitbucket Data
                DataPerSprint bitbucketScmData = bitbucketFuture.get();
                if (bitbucketScmData != null) {
                    addDataToMergedJsonObject("bitbucket", bitbucketScmData);
                    addCommitsToCommitsMap(bitbucketScmData, commitIdToCommitObjectMap);
                }


            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                tracer.exception("iterateSCMTools", e);
            }
        } catch (Exception e) {
            throw new AgentDataCollectionException(confId, "SCM", e);
        } finally {
            executorService.shutdown();
        }
    }

    private void addCommitsToCommitsMap(DataPerSprint scmData, HashMap<String, JSONObject>
            commitIdToCommitObjectMap) throws JSONException {
        for (JSONArray commitsArray : scmData.getDataPerSprint().values()){
            for (int i=0; i< commitsArray.length(); i++){
                JSONObject currCommit = commitsArray.getJSONObject(i);
                String commitId = extractCommitId(currCommit);
                if (!StringUtils.isEmpty(commitId)) {
                    commitIdToCommitObjectMap.put(commitId, currCommit);
                } else{
                    tracer.warn("Unable to extract commit id from commit object. commit object: " + currCommit);
                }
            }
        }
    }

    private String extractCommitId(JSONObject currCommit) throws JSONException {
        // GitHub syntax
        if (currCommit.has("sha")){
            return currCommit.getString("sha");
        }

        // Bitbucket syntax
        if (currCommit.has("hash")){
            return currCommit.getString("hash");
        }

        // GitLab syntax
        return currCommit.getString("id");

    }

    private void buildMergedDataObject() throws JSONException {
        JSONObject externalWrapperObj = new JSONObject();
        JSONObject externalDataObj = new JSONObject();
        JSONObject externalDataContentObj = new JSONObject();
        externalWrapperObj.put("externalData", externalDataObj);
        externalDataObj.put("sprintContent", externalDataContentObj);
        mergedData.put("externalData", externalDataObj);
        mergedData.put("users", new JSONArray());
        mergedData.put("sprints", new JSONArray());
    }

    private void saveDataToDB(CookieStore cookieStore) throws Exception {
        saveSprints(cookieStore);
        saveUsersAndExternalData(cookieStore);
    }

    private void saveSprints(CookieStore cookieStore) throws Exception {
        JSONArray sprintsArray = mergedData.getJSONArray("sprints");
        for (int i = 0; i < sprintsArray.length(); i++) {
            JSONObject wrapperObject = new JSONObject();
            JSONObject currSprint = sprintsArray.getJSONObject(i);
            JSONArray wrapperArray = new JSONArray();
            wrapperArray.put(currSprint);
            wrapperObject.put("configuration_id", confId);
            wrapperObject.put("sprints", wrapperArray);
            saveDataToDB(wrapperObject, cookieStore);
        }
    }

    private void saveUsersAndExternalData(CookieStore cookieStore) throws Exception {
        JSONObject objectToSave = new JSONObject();
        objectToSave.put("users", mergedData.getJSONArray("users"));
        objectToSave.put("sprintsMetaData", mergedData.getJSONArray("sprintsMetaData"));
        objectToSave.put("externalData", mergedData.getJSONObject("externalData"));
        objectToSave.put("configuration_id", confId);
        objectToSave.put("executionTime", Calendar.getInstance().getTime());
        saveDataToDB(objectToSave, cookieStore);
    }

    private void saveDataToDB(JSONObject dataObj, CookieStore cookieStore) throws Exception {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            // create the request
            String serviceURL = getServerBaseURL() + "/" + SERVICE_NAME;
            HttpPost dataRequest = new HttpPost(serviceURL);

            httpClient.setCookieStore(cookieStore);
            StringEntity params = new StringEntity(dataObj.toString());
            dataRequest.addHeader("content-type", "application/x-www-form-urlencoded");

            dataRequest.setEntity(params);
            HttpResponse response = httpClient.execute(dataRequest);
            tracer.info("SERVER RESPONSE STRING: " + response.toString());

            if (response.getStatusLine().getStatusCode() == 302) {
                tracer.warn("getting 302...");
                tracer.warn("Failed to send json to SnapGlue server...");
                throw new AgentDataCollectionException(confId, "Save Data to DB");
            } else if (response.getStatusLine().getStatusCode() == 200) {
                tracer.info("Success to send json to SnapGlue server...");
            } else {
                tracer.warn("Failed to send json to SnapGlue server...");
                tracer.warn("Status code is:" + response.getStatusLine().getStatusCode());
                throw new AgentDataCollectionException(confId, "Save Data to DB");
            }
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    private AgentLastExecutionData getLastExecutionData(CookieStore cookieStore, String configurationID)
            throws Exception {
        tracer.entry("getLastExecutionTime");
        DefaultHttpClient httpClient = new DefaultHttpClient();

        // initialize the date with 1970 first time known to JAVA
        Date lastExecutionDate = new Date(1);
        int lastBuildNumberInt = 0;
        try {
            String url = getServerBaseURL() + "/pluginsService";
            HttpPost request = new HttpPost(url);

            httpClient.setCookieStore(cookieStore);
            request.addHeader("content-type", "application/x-www-form-urlencoded");

            JSONObject objectToSave = new JSONObject();
            objectToSave.put("method", "getLastExecutionData");
            objectToSave.put("configuration_id", configurationID);
            StringEntity params = new StringEntity(objectToSave.toString());
            request.setEntity(params);

            HttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();

            String responseString = EntityUtils.toString(entity, "UTF-8");
            if (responseString == null || responseString.length() == 0) {
                tracer.trace("last execution time of agent not found");
                tracer.exit("getLastExecutionTime");
                return new AgentLastExecutionData(null, -1);
            }

            int beginIndex = responseString.indexOf("<h1>") + 4;
            int endIndex = responseString.indexOf("</h1>");
            if (beginIndex > -1 && endIndex > -1) {
                try {
                    String lastExecutionTime = responseString.substring(beginIndex, endIndex);
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(Long.parseLong(lastExecutionTime));
                    lastExecutionDate = cal.getTime();
                } catch (Throwable e) {
                    // do nothing, lastExecutionDate is set to the beginning of time
                }
            }

            //get the build number
            beginIndex = responseString.indexOf("<h2>") + 4;
            endIndex = responseString.indexOf("</h2>");
            if (endIndex > -1) {
                String lastBuildNumberString = responseString.substring(beginIndex, endIndex);
                if (!isEmpty(lastBuildNumberString) && !lastBuildNumberString.contains("null")) {
                    lastBuildNumberInt = Integer.valueOf(lastBuildNumberString);
                }
            }
        } catch (Exception e) {
            tracer.trace("Failed to get the last execution time. The reason is: " + e);
            e.printStackTrace();
        } finally {
            httpClient.getConnectionManager().shutdown();
        }

        tracer.exit("getLastExecutionTime");
        return new AgentLastExecutionData(lastExecutionDate, lastBuildNumberInt);
    }

    private void iterateJira(org.bson.Document doc) throws AgentDataCollectionException {
        tracer.trace("-------------Jira-------------");
        if (doc == null) {
            tracer.warn("Tools document is null");
            return;
        }
        org.bson.Document jiraDoc = (org.bson.Document) doc.get(MONGODB_CONFIGURATION_TOOL_NAME_JIRA);
        if (jiraDoc == null || jiraDoc.isEmpty()) {
            tracer.warn("JIRA document is null");
            return;
        }
        try {
            String jiraServerUri = (String) jiraDoc.get(MONGODB_CONFIGURATION_JIRA_JIRA_SERVER_URI);
            String projectName = (String) jiraDoc.get(MONGODB_CONFIGURATION_JIRA_PROJECT_NAME);
            String username = (String) jiraDoc.get(MONGODB_CONFIGURATION_JIRA_USER_NAME);
            String password = (String) jiraDoc.get(MONGODB_CONFIGURATION_JIRA_PASSWORD);
            tracer.trace("Jira uri: " + jiraServerUri);
            JiraIssuesRetriever jiraIssuesRetriever = new JiraIssuesRetriever(jiraServerUri, projectName, username,
                    password, userManager);

            sprintsDatesMap = new HashMap<>();
            Set<String> closedSprintsNames = new HashSet<>();
            JSONArray jiraIssuesData = jiraIssuesRetriever.retrieveJIRAData(lastExecutionTime, jiraToSprintMap,
                    sprintsDatesMap, closedSprintsNames);
            Date earliestSprintDate = getEarliestStartDate();
            if (earliestSprintDate != null && (lastExecutionTime == null || lastExecutionTime.before
                    (earliestSprintDate))) {
                lastExecutionTime = earliestSprintDate;
            }
            tracer.info("Jira number of sprints (including external) is: " + sprintsDatesMap.size());
            addSprintsToMergedData(jiraIssuesData);
            JSONArray closedSprints = createClosedSprintsArray(closedSprintsNames);
            addClosedToMergedData(closedSprints);
        } catch (Exception e) {
            throw new AgentDataCollectionException(confId, "JIRA", e);
        }
    }

    public JSONArray createClosedSprintsArray(Set<String> closedSprintsNames) throws JSONException {
        JSONArray closedSprintArray = new JSONArray();
        for (String sprintName : closedSprintsNames){
            JSONObject closedSprintObj = createClosedSprintJSONObject(sprintName);
            closedSprintArray.put(closedSprintObj);
        }
        return closedSprintArray;
    }

    private JSONObject createClosedSprintJSONObject(String sprintName) throws JSONException {
        JSONObject closedSprint = new JSONObject();
        closedSprint.put("name", sprintName);
        closedSprint.put("state", "CLOSED");
        return closedSprint;
    }

    private void addClosedToMergedData(JSONArray closedSprints) throws JSONException {
        mergedData.put("sprintsMetaData", closedSprints);
    }

    private Date getEarliestStartDate() {
        Date earliestDate = null;
        for (Pair<Date, Date> currDatesPair: sprintsDatesMap.values()){
            Date currDate = currDatesPair.getLeft();
            if (earliestDate == null || earliestDate.after(currDate)){
                earliestDate = currDate;
            }
        }
        if (earliestDate == null){
            return  null;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(earliestDate);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }


    private void addSprintsToMergedData(JSONArray jiraIssuesData) throws JSONException {
        JSONArray sprintsArray = mergedData.getJSONArray("sprints");
        int i;
        for (i = 0; i < jiraIssuesData.length(); i++) {
            JSONObject currSprintObj = jiraIssuesData.getJSONObject(i);
            JSONObject newSprintObj = new JSONObject();
            JSONObject sprintContentObj = new JSONObject();
            newSprintObj.put("sprintInfo", currSprintObj.get("sprint"));
            sprintContentObj.put("jira", currSprintObj.getJSONObject("contents"));
            newSprintObj.put("sprintContent", sprintContentObj);
            sprintsArray.put(newSprintObj);
        }
        tracer.info("Number of Sprints fetched from JIRA:" + i);
    }

    private void addDataToMergedJsonObject(String toolName, DataPerSprint dataPerSprint) throws JSONException {
        Map<String, JSONArray> sprintToDataMap = dataPerSprint.getDataPerSprint();
        JSONObject sprintObj;
        for (String sprintName : sprintToDataMap.keySet()) {
            sprintObj = getSprintObject(sprintName);
            JSONObject sprintContentArray = sprintObj.getJSONObject("sprintContent");
            JSONArray contentArray = sprintToDataMap.get(sprintName);
            sprintContentArray.put(toolName, contentArray);
            tracer.info("Tool \"" + toolName + "\" data size for sprint \"" + sprintName + "\" is " +
                    contentArray
                            .length());
        }
    }

    private JSONObject getSprintObject(String sprintName) throws JSONException {
        JSONObject sprintObj = null;
        if ("externalData".equals(sprintName)) {
            sprintObj = mergedData.getJSONObject(sprintName);
        } else {
            JSONArray sprintArray = mergedData.getJSONArray("sprints");
            for (int i = 0; i < sprintArray.length(); i++) {
                JSONObject currSprint = sprintArray.getJSONObject(i);
                JSONObject sprintInfoObj = currSprint.getJSONObject("sprintInfo");
                String currSprintName = sprintInfoObj.getString("name");
                if (sprintName.equals(currSprintName)) {
                    sprintObj = currSprint;
                    break;
                }
            }
        }
        return sprintObj;
    }

    @Override
    public void interrupt() throws UnableToInterruptJobException {
        {
            stopFlag.set(true);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                tracer.exception("interrupt()", e);
            }

            Thread thread = runningThread.getAndSet(null);
            if (thread != null)
                thread.interrupt();
        }
    }

    @Override
    public void run() {
        collectDataSync();
    }

    private static class AgentLastExecutionData {
        private Date lastExecutionTime;
        private int lastBuildNumber;

        public AgentLastExecutionData(Date lastExecutionTime, int lastBuildNumber) {
            this.lastExecutionTime = lastExecutionTime;
            this.lastBuildNumber = lastBuildNumber;
        }

        /**
         * @return the lastExecutionTime
         */
        public Date getLastExecutionTime() {
            return lastExecutionTime;
        }

        /**
         * @return the lastBuildNumber
         */
        public int getLastBuildNumber() {
            return lastBuildNumber;
        }
    }

    private static String getServerBaseURL() {
        String url = "http://" + host;
        if (!isEmpty(port)) {
            url += ":" + port;
        }
        if (!isEmpty(serverPath)) {
            url += "/" + serverPath;
        }
        return url;
    }

    public static void main(String[] args) {

//		try
//		{
//			doLogin();
//			sendGetRequest();
//		}
//		catch (Exception e)
//		{
//			// LY TBD Auto-generated catch block
//			e.printStackTrace();
//		}

//        MongoDBAgentServiceImpl impl = new MongoDBAgentServiceImpl();
//        impl.getMongoDatabase().drop();

        /*String docStr = "{\"project\": \"snapglue\",\"version\": \"1.0\",\"startDate\": \"1/9/2015\",\"endDate\": " +
                "\"30/4/2015\",\"sprintName\": \"sprint 5\",\"sprintStartDate\": \"1/9/2015\",\"sprintEndDate\": " +
                "\"22/9/2015\",\"tools\": {\"Jenkins\": {\"hostname\": \"localhost\",\"port\": \"8080\",\"jobName\": " +
                "\"SNAPGLUE\"},\"Jira\": {\"jiraServerUri\": \"http://snapglue.ddns.net:9090/\",\"userName\": " +
                "\"avia\",\"password\": \"1q2w3e4r\",\"projectName\": \"SNAPGLUE\",\"versionNumber\": \"1.0\"}," +
                "\"Gitlab\": {\"serverUrl\": \"http://snapglue.ddns.net:7171\",\"token\": \"JDg_stmCLqZFFatBJDUj\"," +
                "\"projectName\": \"SnapGlue\",\"branchName\": \"master\",\"useOAuth2\": \"false\"},\"SVN\": " +
                "{\"userName\": \"avia\",\"password\": \"1q2w3e4r\",\"repositoryURL\": \"???\",\"branchName\": " +
                "\"master\",},\"Bamboo\": {\"hostname\": \"localhost\",\"port\": \"8080\",\"userName\": \"SnapGlue\"," +
                "\"password\": \"master\",\"planKey\": \"planKey\",\"projectKey\": \"projectKey\"},\"Github\": " +
                "{\"userName\": \"user1\",\"password\": \"pass1\",\"repositoryName\": \"SnapGlue_repo\"," +
                "\"repositoryOwner\": \"owner\",\"branchName\": \"branch1\"}}}";

        impl.addConfiguration(docStr);*/

//        PluginsCollectorJob job = new PluginsCollectorJob();
//        try {
//            job.iterateToolsAndRetrieveData(null, -1);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        tracer.trace("created");
    }

}

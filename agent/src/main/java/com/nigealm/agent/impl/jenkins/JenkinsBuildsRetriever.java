package com.nigealm.agent.impl.jenkins;

import com.nigealm.agent.impl.AgentDataCollectionException;
import com.nigealm.agent.impl.CommonCollectorsUtils;
import com.nigealm.agent.impl.DataPerSprint;
import com.nigealm.agent.users.UserManager;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class JenkinsBuildsRetriever extends AbstractJenkinsDataRetriever {

    public JenkinsBuildsRetriever(String hostname, String port, String jobName) {
        super(hostname, port, jobName);
    }

    public JenkinsBuildsRetriever(String hostname, String port, String jobName,
                                  String user, String password) {
        super(hostname, port, jobName, user, password);
    }

    public static void main(String args[]) throws JSONException, ExecutionException, IOException {
        JenkinsBuildsRetriever retriever = new JenkinsBuildsRetriever("https://ci.jenkins-ci.org", "", "jenkins_2.0");

        UserManager userManager = new UserManager();
        userManager.createNewUser("daniel-beck", "", "daniel-beck");
        userManager.createNewUser("James Nord", "", "James Nord");
        userManager.createNewUser("Jesse Glick", "", "Jesse Glick");
        userManager.createNewUser("Kohsuke Kawaguchi", "", "Kohsuke Kawaguchi");

        retriever.retrieveJenkinsBuilds(true, -1, userManager, new ConcurrentHashMap<String, Set<String>>(),
                new ConcurrentHashMap<String, Set<String>>(), new ConcurrentHashMap<String, Set<String>>(), new
                        HashMap<String, JSONObject>(), new ConcurrentSkipListMap<String, Pair<Date, Date>>());
    }


    public DataPerSprint retrieveJenkinsBuilds(boolean retrieveAllData, int lastBuildNumber, UserManager userManager,
                                               Map<String, Set<String>> githubCommitToSprintMap, Map<String,
            Set<String>> gitlabCommitToSprintMap, Map<String, Set<String>> bitbucketCommitToSprintMap,
                                               HashMap<String, JSONObject> commitIdToCommitObjectMap, Map<String,
            Pair<Date, Date>> sprintsDatesMap) throws
            IOException,
            ExecutionException, JSONException {
        if (retrieveAllData) {
            lastBuildNumber = -1;
        }
        String dataJson = getDataJsonFromJenkinsServer();
        return buildReturnedObject(dataJson, lastBuildNumber, userManager, githubCommitToSprintMap,
                gitlabCommitToSprintMap, bitbucketCommitToSprintMap, commitIdToCommitObjectMap, sprintsDatesMap);
    }

    public DataPerSprint retrieveJenkinsBuildsWithAuth(boolean retrieveAllData, int lastBuildNumber, UserManager
            userManager, Map<String, Set<String>> githubCommitToSprintMap, Map<String, Set<String>>
                                                               gitlabCommitToSprintMap, Map<String, Set<String>>
                                                               bitbucketCommitToSprintMap, HashMap<String, JSONObject>
                                                               commitIdToCommitObjectMap, Map<String,
            Pair<Date, Date>> sprintsDatesMap) throws IOException,
            ExecutionException, JSONException, AgentDataCollectionException {
        if (retrieveAllData) {
            lastBuildNumber = -1;
        }
        String dataJson = retrieveJenkinsBuildsWithAuth();
        return buildReturnedObject(dataJson, lastBuildNumber, userManager, githubCommitToSprintMap,
                gitlabCommitToSprintMap, bitbucketCommitToSprintMap, commitIdToCommitObjectMap, sprintsDatesMap);
    }

    private DataPerSprint buildReturnedObject(String buildsJsonString, int lastBuildNumber, UserManager userManager,
                                              Map<String, Set<String>> githubCommitToSprintMap,
                                              Map<String, Set<String>> gitlabCommitToSprintMap,
                                              Map<String, Set<String>> bitbucketCommitToSprintMap,
                                              HashMap<String, JSONObject> commitIdToCommitObjectMap,
                                              Map<String, Pair<Date, Date>> sprintsDatesMap) throws
            ExecutionException, JSONException {
        DataPerSprint dataPerSprint = new DataPerSprint();
        ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
        if (buildsJsonString == null) {
            tracer.warn("Jenkins builds json is null!");
            return dataPerSprint;
        }
        try {
            JSONObject jsonBuildsObject = new JSONObject(buildsJsonString);
            JSONArray tempArray = (JSONArray) (jsonBuildsObject.get("builds"));
            List<Future<JSONArray>> buildsFutureList = new LinkedList<>();
            for (int i = 0; i < tempArray.length(); i++) {
                JSONObject currBuild = (JSONObject) tempArray.get(i);
                Future<JSONArray> currBuildFuture = executorService.submit(new JenkinsBuildRetrieverCallable
                        (currBuild, userManager, hostname, port, jobName, user, password));
                buildsFutureList.add(currBuildFuture);
            }

            Set<String> allSprintNamesSet = new HashSet<>();
            JSONArray allBuilds = new JSONArray();
            for (Future<JSONArray> currBuildFuture : buildsFutureList) {
                JSONArray currBuildsArray = currBuildFuture.get();
                if (currBuildsArray == null) {
                    continue;
                }

                addBuildsToCommits(currBuildsArray, commitIdToCommitObjectMap);
                extractSprintNames(allSprintNamesSet, currBuildsArray, githubCommitToSprintMap,
                        gitlabCommitToSprintMap, bitbucketCommitToSprintMap, sprintsDatesMap);

                for (int i = 0; i < currBuildsArray.length(); i++) {
                    JSONObject currBuildObj = currBuildsArray.getJSONObject(i);
                    int buildNumber = currBuildObj.getInt("number");
                    if (buildNumber > lastBuildNumber) {
                        allBuilds.put(currBuildObj);
                    }
                }
            }

            if (allSprintNamesSet.isEmpty()) {
                allSprintNamesSet.add("externalData");
            }

            for (String sprintName : allSprintNamesSet) {
                for (int i = 0; i < allBuilds.length(); i++) {
                    dataPerSprint.addData(sprintName, allBuilds.getJSONObject(i));
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            tracer.exception("buildReturnedObject", e);
        } finally {
            executorService.shutdown();
        }
        return dataPerSprint;
    }

    private void addBuildsToCommits(JSONArray buildsArray, HashMap<String, JSONObject> commitIdToCommitObjectMap)
            throws JSONException {
        for (int j = 0; j < buildsArray.length(); j++) {
            JSONObject currBuildObj = buildsArray.getJSONObject(j);
            JSONObject changeSetObj = currBuildObj.getJSONObject("changeSet");
            JSONArray itemsArray = changeSetObj.getJSONArray("items");
            for (int i = 0; i < itemsArray.length(); i++) {
                JSONObject currItem = itemsArray.getJSONObject(i);
                String commitId = currItem.getString("id");
                if (!StringUtils.isEmpty(commitId)) {
                    JSONObject commitObj = commitIdToCommitObjectMap.get(commitId);
                    if (commitObj != null) {
                        commitObj.put("related_build", currBuildObj.getInt("number"));
                    }
                }
            }
        }
    }

    private void extractSprintNames(Set<String> allSprintNamesSet, JSONArray buildsArray, Map<String, Set<String>>
            githubCommitToSprintMap, Map<String, Set<String>> gitlabCommitToSprintMap, Map<String, Set<String>>
                                            bitbucketCommitToSprintMap, Map<String, Pair<Date, Date>> sprintsDatesMap) throws
            JSONException {

        for (int j = 0; j < buildsArray.length(); j++) {
            JSONObject currBuildObj = buildsArray.getJSONObject(j);
            JSONObject changeSetObj = currBuildObj.getJSONObject("changeSet");
            JSONArray itemsArray = changeSetObj.getJSONArray("items");
            long timestamp = currBuildObj.getLong("timestamp");
            Date buildDate = new Date(timestamp);
            List<String> potentialSprintsNames = CommonCollectorsUtils.getPotentialSprints(buildDate, sprintsDatesMap);
            for (int i = 0; i < itemsArray.length(); i++) {
                JSONObject currItem = itemsArray.getJSONObject(i);
                String commitId = currItem.getString("id");
                Set<String> sprintsNames = githubCommitToSprintMap.get(commitId);
                if (sprintsNames == null) {
                    sprintsNames = gitlabCommitToSprintMap.get(commitId);
                }
                if (sprintsNames == null) {
                    sprintsNames = bitbucketCommitToSprintMap.get(commitId);
                }
                if (sprintsNames != null) {
                    sprintsNames.remove("externalData");
                    allSprintNamesSet.addAll(ListUtils.intersection(potentialSprintsNames, Arrays.asList(sprintsNames
                            .toArray(new String[0]))));
                }
            }
        }
    }

}

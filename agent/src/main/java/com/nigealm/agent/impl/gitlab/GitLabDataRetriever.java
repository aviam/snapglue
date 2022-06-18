package com.nigealm.agent.impl.gitlab;

import com.nigealm.agent.impl.AgentDataCollectionException;
import com.nigealm.agent.impl.DataPerSprint;
import com.nigealm.agent.impl.JsonUtils;
import com.nigealm.agent.users.UserManager;
import com.nigealm.common.utils.DateUtils;
import com.nigealm.plugins.scm.gitlab.Project;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

import static com.nigealm.agent.impl.CommonCollectorsUtils.DAYS_LIMIT;
import static com.nigealm.agent.impl.JsonUtils.parseJson;

public class GitLabDataRetriever extends AbstractGitLabDataRetriever {

    private final static int COMMIT_FETCHER_THREAD_COUNT = 5;

    private final static int NUMBER_OF_COMMITS_PER_PAGE = 500;

    private static final Object syncObj = new Object();

    public GitLabDataRetriever(String confId, String url, String token, String projectName) {
        super(confId, token, url + "/api/v3/", projectName);
    }

    public static void main(String args[]) throws ExecutionException, AgentDataCollectionException, JSONException {
        GitLabDataRetriever retriever = new GitLabDataRetriever("", "http://snapglue.ddns.net:7070",
                "dp17GHxNSVUqyzesjc36", "SnapGlue");
        retriever.getCommits(null, new HashMap<String, Pair<Date, Date>>(),
                new ConcurrentHashMap<String, Set<String>>(),
                new ConcurrentHashMap<String, Set<String>>(),
                new UserManager());
    }

    public DataPerSprint getCommits(Date lastIteration, Map<String, Pair<Date, Date>> sprintsDatesMap,
                                    Map<String, Set<String>> gitlabCommitToSprintMap,
                                    Map<String, Set<String>> jiraToSprintMap, UserManager userManager)
            throws ExecutionException, AgentDataCollectionException, JSONException {
        DataPerSprint dataPerSprint = new DataPerSprint();
        String projectId = getProjectId();
        JSONArray branchesArray = getAllBranches(projectId);
        for (int i = 0; i < branchesArray.length(); i++) {
            initSyncAndStopObjects();
            JSONObject currBranchObj = branchesArray.getJSONObject(i);
            String branchName = currBranchObj.getString("name");
            JSONArray commitsArray = getAllCommits(gitlabCommitToSprintMap, sprintsDatesMap, jiraToSprintMap,
                    userManager, projectId, branchName);
            DataPerSprint currDataPerSPrint = filterCommitsByLastIterationDate(commitsArray, lastIteration,
                    gitlabCommitToSprintMap);
            dataPerSprint.mergeData(currDataPerSPrint);
        }
        return dataPerSprint;
    }

    private JSONArray getAllBranches(String projectId) throws AgentDataCollectionException, JSONException {
        String customUrl = baseUrl + "projects/" + projectId + "/repository/branches";
        String branchesJson = retrieveData(customUrl);
        return JsonUtils.getJSONArrayFromResponse(branchesJson);
    }


    public JSONArray getAllCommits(Map<String, Set<String>> gitlabCommitToSprintMap, Map<String, Pair<Date, Date>>
            sprintsDatesMap, Map<String, Set<String>> jiraToSprintMap, UserManager userManager, String
            projectId, String branchName) throws AgentDataCollectionException, ExecutionException, JSONException {
        numberOfRunningRequests = 0;
        ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool
                (COMMIT_FETCHER_THREAD_COUNT);
        List<Future<JSONArray>> commitsFutureList = new LinkedList<>();
        JSONArray commitsArray = new JSONArray();
        String customUrl = buildUrlForCommits(projectId, branchName);
        int currentPage = 0;

        while (!shouldStopFetchingCommits()) {
            String nextUrl = customUrl + currentPage;
            GitLabCommitsFetcherCallable currFetcher = new GitLabCommitsFetcherCallable(confId, this, token, baseUrl,
                    projectName, nextUrl, sprintsDatesMap, gitlabCommitToSprintMap, jiraToSprintMap, userManager);
            Future<JSONArray> future = submitTask(executorService, currFetcher);
            if (future != null) {
                commitsFutureList.add(future);
            }
            currentPage++;
        }

        try {
            for (Future<JSONArray> future : commitsFutureList) {
                JSONArray currData = future.get();
                if (currData != null) {
                    JsonUtils.mergeJSONArrays(commitsArray, currData);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            tracer.exception("getCommits", e);
        } finally {
            cleanup(executorService);
        }
        return commitsArray;
    }

    private DataPerSprint filterCommitsByLastIterationDate(JSONArray commitsArray, Date lastIterationDate,
                                                           Map<String, Set<String>> gitlabCommitToSprintMap) throws
            JSONException {
        DataPerSprint dataPerSprint = new DataPerSprint();
        for (int i = 0; i < commitsArray.length(); i++) {
            JSONObject commitObj = commitsArray.getJSONObject(i);
            String time = (String) commitObj.get("created_at");
            Date commitDate = DateUtils.parseDateToLocalTime(time);
            if ((commitDate != null) && (lastIterationDate == null || commitDate.after(lastIterationDate))) {
                String commitId = commitObj.getString("id");
                Set<String> sprintNames = gitlabCommitToSprintMap.get(commitId);
                for (String sprintName : sprintNames) {
                    dataPerSprint.addData(sprintName, commitObj);
                }
            }
        }

        return dataPerSprint;
    }

    private void cleanup(ExecutorService executorService) throws JSONException {
        super.cleanup();
        tracer.trace("GitLabDataRetriever: shutdown executor service");
        executorService.shutdown();
    }

    private Future<JSONArray> submitTask(ExecutorService executorService, GitLabCommitsFetcherCallable
            currFetcher) {
        synchronized (syncObj) {
            try {
                while (numberOfRunningRequests >= COMMIT_FETCHER_THREAD_COUNT) {
                    syncObj.wait();
                }
                numberOfRunningRequests++;
                return executorService.submit(currFetcher);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                tracer.exception("submitTask", e);
            }
        }
        return null;
    }

    public void decreaseNumberOfRunningFlow() {
        synchronized (syncObj) {
            numberOfRunningRequests--;
            syncObj.notify();
        }
    }

    private String buildUrlForCommits(String projectId, String branchName) {
        String customUrl = baseUrl + "projects/" + projectId + "/repository/commits";
        customUrl += "?ref_name=" + branchName;
        String sinceDate = getSinceDate();
        customUrl += "&since=" + sinceDate;
        customUrl += "&per_page=" + NUMBER_OF_COMMITS_PER_PAGE + "&page=";
        return customUrl;
    }

    private String getSinceDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1 * DAYS_LIMIT);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        return dateFormat.format(cal.getTime());
    }

    private String getProjectId() throws AgentDataCollectionException {
        String customUrl = baseUrl + "projects";
        String projectsJson = retrieveData(customUrl);
        List<Project> projectsList = parseJson(projectsJson, Project.class);
        for (Project project : projectsList) {
            if (projectName.equalsIgnoreCase(project.getName())) {
                return project.getId();
            }
        }

        tracer.warn("Returning null project id");
        return "null";
    }
}

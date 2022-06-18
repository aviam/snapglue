package com.nigealm.agent.impl.github;

import com.nigealm.agent.impl.CommonCollectorsUtils;
import com.nigealm.agent.impl.DataPerSprint;
import com.nigealm.agent.impl.JsonUtils;
import com.nigealm.agent.users.User;
import com.nigealm.agent.users.UserManager;
import com.nigealm.common.utils.DateUtils;
import com.nigealm.common.utils.Tracer;
import com.sun.jersey.core.util.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.nigealm.agent.impl.CommonCollectorsUtils.*;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class GitHubDataRetriever {

    protected static final String HEADER_AUTHORIZATION = "Authorization";
    private final static int DAYS_LIMIT = 90;
    private static final Tracer tracer = new Tracer(GitHubDataRetriever.class);
    private static final int COMMITS_PER_PAGE = 100;
    private static final String BASE_URL = "https://api.github.com/repos/";
    private String username;
    private String password;
    private String oauthToken;

    public GitHubDataRetriever(String username, String password, String oauthToken) {
        this.username = username;
        this.password = password;
        this.oauthToken = oauthToken;
    }

    public static void main(String args[]) throws JSONException, IOException {
        GitHubDataRetriever retriever = new GitHubDataRetriever("", "",
                "72653852816f022b2cf397d76b13e1f7c4814a2a");
        retriever.getCommitsSince("gilams/SnapGlue", null, new UserManager(), new ConcurrentHashMap<String,
                Set<String>>(), new ConcurrentHashMap<String, Set<String>>(), new HashMap<String, Pair<Date, Date>>());
    }

    public DataPerSprint getCommitsSince(String repoName, Date lastIterationDate,
                                         UserManager userManager, Map<String, Set<String>>
                                                 jiraToSprintMap, Map<String, Set<String>>
                                                 githubCommitToSprintMap, Map<String, Pair<Date, Date>>
                                                 sprintsDatesMap) throws JSONException, IOException {
        JSONArray commitsArray = getCommitsAndBuildMap(repoName, userManager, jiraToSprintMap,
                githubCommitToSprintMap, sprintsDatesMap);
        commitsArray = filterMergeCommits(commitsArray);
        return filterCommitsByLastIterationDate(commitsArray, lastIterationDate, githubCommitToSprintMap);
    }

    private JSONArray filterMergeCommits(JSONArray commitsArray) throws JSONException {
        JSONArray filteredArray = new JSONArray();
        for (int i = 0; i < commitsArray.length(); i++) {
            JSONObject currCommitObj = commitsArray.getJSONObject(i);
            JSONObject commitObj = currCommitObj.getJSONObject("commit");
            String commitMsg = commitObj.getString("message");
            if (!isMergeCommit(commitMsg)) {
                filteredArray.put(currCommitObj);
            }
        }
        return filteredArray;
    }

    private DataPerSprint filterCommitsByLastIterationDate(JSONArray commitsArray, Date lastIterationDate,
                                                           Map<String, Set<String>> githubCommitToSprintMap) throws
            JSONException {
        DataPerSprint dataPerSprint = new DataPerSprint();
        for (int i = 0; i < commitsArray.length(); i++) {
            JSONObject currCommitObj = commitsArray.getJSONObject(i);
            Date commitDate = geCommitDate(currCommitObj);
            if (commitDate != null && (lastIterationDate == null || commitDate.after(lastIterationDate))) {
                String commitId = currCommitObj.getString("sha");
                Set<String> sprintsNames = githubCommitToSprintMap.get(commitId);
                for (String sprintName : sprintsNames) {
                    dataPerSprint.addData(sprintName, currCommitObj);
                }
            }
        }

        return dataPerSprint;
    }

    private Date geCommitDate(JSONObject currCommitObj) throws JSONException {
        JSONObject commitObj = currCommitObj.getJSONObject("commit");
        JSONObject committerObj = commitObj.getJSONObject("committer");
        String commitDateStr = committerObj.getString("date");
        return DateUtils.parseDateToLocalTime(commitDateStr);
    }

    private Date getSinceDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1 * DAYS_LIMIT);
        return cal.getTime();
    }

    private JSONArray getCommitsAndBuildMap(String repoName, UserManager userManager,
                                            Map<String, Set<String>> jiraToSprintMap,
                                            Map<String, Set<String>> githubCommitToSprintMap, Map<String, Pair<Date,
            Date>> sprintsDatesMap) throws
            JSONException, IOException {
        String customUrl = buildURL(repoName, getSinceDate());
        JSONArray commitsArray = getCommitsPaged(customUrl);
        commitsArray = filterMergeCommits(commitsArray);
        addCommitsToCommitToSprintMap(jiraToSprintMap, githubCommitToSprintMap, commitsArray, sprintsDatesMap);
        addGitHubUsers(commitsArray, userManager);
        return commitsArray;
    }

    private void addCommitsToCommitToSprintMap(Map<String, Set<String>> jiraToSprintMap, Map<String, Set<String>>
            githubCommitToSprintMap, JSONArray commitsArray, Map<String, Pair<Date, Date>> sprintsDatesMap) throws
            JSONException {
        for (int i = 0; i < commitsArray.length(); i++) {
            JSONObject currCommitObj = commitsArray.getJSONObject(i);
            String commitId = currCommitObj.getString("sha");
            JSONObject commitObj = currCommitObj.getJSONObject("commit");
            String commitMsg = commitObj.getString("message");
            List<String> commitIssues = getIssueFromCommitMessage(commitMsg, jiraToSprintMap.keySet());
            currCommitObj.put("related_issues", commitIssues);
            Set<String> sprintsNames = extractSprintsNames(jiraToSprintMap, sprintsDatesMap, currCommitObj,
                    commitIssues);
            githubCommitToSprintMap.put(commitId, sprintsNames);
        }
    }

    private Set<String> extractSprintsNames(Map<String, Set<String>> jiraToSprintMap, Map<String, Pair<Date,
            Date>> sprintsDatesMap, JSONObject currCommitObj, List<String> commitIssues) throws JSONException {
        Set<String> sprintsNames = new HashSet<>();
        Date commitDate = geCommitDate(currCommitObj);

        List<String> potentialSprints = getPotentialSprints(commitDate, sprintsDatesMap);
        for (String commitIssue : commitIssues) {
            List<String> sprintNamesList = CommonCollectorsUtils.getSprintNames(commitIssue, potentialSprints,
                    jiraToSprintMap);
            if (sprintNamesList != null) {
                sprintsNames.addAll(sprintNamesList);
            }
        }

        if (sprintsNames.isEmpty()) {
            sprintsNames.add("externalData");
        }

        return sprintsNames;
    }

    private String buildURL(String repoName, Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        String customUrl = BASE_URL + repoName + "/commits?since=";
        customUrl += cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar
                .DAY_OF_MONTH);
        customUrl += "T" + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + ":00Z";
        customUrl += "page=1&per_page=" + COMMITS_PER_PAGE;
        tracer.trace("GitHub url to fetch commits: " + customUrl);
        return customUrl;
    }

//    private String getCommitMessageFromCommitObj(JSONObject currCommitObj) throws JSONException {
//        String commitMsg = "";
//        if (currCommitObj.has("message")) {
//            commitMsg = currCommitObj.getString("message");
//        } else if (currCommitObj.has("commit")) {
//            JSONObject commitFieldObj = currCommitObj.getJSONObject("commit");
//            if (commitFieldObj.has("message")) {
//                commitMsg = commitFieldObj.getString("message");
//            }
//        }
//        return commitMsg;
//    }

    private JSONArray getCommitsPaged(String customUrl) throws JSONException, IOException {
        JSONArray commitsArray = new JSONArray();
        String linkResponse;
        boolean isLast = false;
        while (!isLast && !StringUtils.isEmpty(customUrl)) {
            linkResponse = retrieveData(customUrl, commitsArray);
            isLast = isLastPage(linkResponse);
            if (!isLast) {
                customUrl = extractUrlFromLinkResponse(linkResponse);
            }
        }
        return commitsArray;
    }

    private String extractUrlFromLinkResponse(String linkResponse) {
        int begin = linkResponse.indexOf('<') + 1;
        int end = linkResponse.indexOf('>');
        if (begin != -1 && end != -1) {
            return linkResponse.substring(begin, end);
        }
        return "";
    }

    private boolean isLastPage(String linkResponse) {
        return linkResponse == null || !linkResponse.contains("next");
    }

    private void addGitHubUsers(JSONArray commitsArray, UserManager userManager) {
        for (int i = 0; i < commitsArray.length(); i++) {
            try {
                JSONObject currCommit = (JSONObject) commitsArray.get(i);
                JSONObject commitSubObject = currCommit.getJSONObject("commit");
                JSONObject committerObject = commitSubObject.getJSONObject("committer");

                String fullName = committerObject.getString("name");
                String email = committerObject.getString("email");
                String userName = getUsername(currCommit, email);

                boolean isUserAddedSuccessfully = userManager.addToolToUserByEmailOrFullNameOrUserName("github",
                        userName, email, fullName);
                if (isUserAddedSuccessfully) {
                    User snapglueUser = userManager.matchUserByEmailOrFullNameOrUserName(userName,
                            email,
                            fullName);
                    currCommit.put("snapglueUser", snapglueUser.getFullName());
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private String getUsername(JSONObject currCommit, String userEmail) throws JSONException {
        // in case we failed to retrieve the login username we will use the email as username
        String userName = userEmail;
        Object tempObj = currCommit.get("author");
        if (!(tempObj == null || isEmpty(tempObj.toString()) || tempObj.toString().equals("null"))) {
            JSONObject authorObject = currCommit.getJSONObject("author");
            userName = authorObject.getString("login");
        }
        return userName;
    }

    private String retrieveData(String customUrl, JSONArray dataArray) throws IOException, JSONException {
        URL urlConnection;
        HttpURLConnection connection = null;

        try {
            urlConnection = new URL(customUrl);
            connection = (HttpURLConnection) urlConnection.openConnection();
            connection.setRequestMethod("GET");
            if (!isEmpty(oauthToken)) {
                connection.setRequestProperty(HEADER_AUTHORIZATION, "token " + oauthToken);
            } else if (!(isEmpty(username) || isEmpty(password))) {
                connection.setRequestProperty(HEADER_AUTHORIZATION, "Basic " + Arrays.toString(Base64.encode(username
                        + ":" + password)));
            }

            if (connection.getResponseCode() != 200) {
                tracer.warn("Failed : HTTP error code : "
                        + connection.getResponseCode());
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (connection.getInputStream())));

            String output;
            StringBuilder commits = new StringBuilder();
            while ((output = br.readLine()) != null) {
                commits.append(output);
            }

            JSONArray currJSONArray = JsonUtils.getJSONArrayFromResponse(commits.toString());
            for (int i = 0; i < currJSONArray.length(); i++) {
                dataArray.put(currJSONArray.get(i));
            }

            return connection.getHeaderField("Link");
        } finally {
            if (connection != null)
                connection.disconnect();
        }
    }


}

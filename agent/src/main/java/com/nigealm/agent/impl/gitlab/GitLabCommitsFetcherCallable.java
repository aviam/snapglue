package com.nigealm.agent.impl.gitlab;

import com.nigealm.agent.impl.AgentDataCollectionException;
import com.nigealm.agent.impl.CommonCollectorsUtils;
import com.nigealm.agent.users.User;
import com.nigealm.agent.users.UserManager;
import com.nigealm.common.utils.DateUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import static com.nigealm.agent.impl.CommonCollectorsUtils.*;

public class GitLabCommitsFetcherCallable extends AbstractGitLabDataRetriever implements Callable<JSONArray> {

    private String customUrl;
    private Map<String, Pair<Date, Date>> sprintsDatesMap;
    private Map<String, Set<String>> gitlabCommitToSprintMap;
    private Map<String, Set<String>> jiraSprintMap;
    private UserManager userManager;
    private GitLabDataRetriever owner;

    public GitLabCommitsFetcherCallable(String confId, GitLabDataRetriever owner, String token, String baseUrl, String
            projectName, String customUrl, Map<String, Pair<Date, Date>> sprintsDatesMap, Map<String, Set<String>>
                                                gitlabCommitToSprintMap, Map<String, Set<String>>
            jiraSprintMap, UserManager userManager) {
        super(confId, token, baseUrl, projectName);
        this.owner = owner;
        this.customUrl = customUrl;
        this.sprintsDatesMap = sprintsDatesMap;
        this.gitlabCommitToSprintMap = gitlabCommitToSprintMap;
        this.jiraSprintMap = jiraSprintMap;
        this.userManager = userManager;
    }

    @Override
    public JSONArray call() throws Exception {
        return getCommits();
    }

    private JSONArray getCommits() throws JSONException, ExecutionException, AgentDataCollectionException {
        String commitsJson = retrieveData(customUrl);
        JSONArray commitsArray = new JSONArray(commitsJson);
        String pageNumber = customUrl.substring(customUrl.lastIndexOf('=') + 1);
        if (commitsArray.length() == 0) {
            if (!shouldStopFetchingCommits()) {
                stopFetchingCommits();
            }
            owner.decreaseNumberOfRunningFlow();
            return null;
        }

        JSONArray filteredArray = new JSONArray();
        for (int i = 0; i < commitsArray.length(); i++) {
            JSONObject commitObj = (JSONObject) commitsArray.get(i);
            addGitLabUserName(commitObj, userManager);
            String commitId = commitObj.getString("id");
            String commitMsg = commitObj.getString("title");
            if (isMergeCommit(commitMsg)) {
                continue;
            }

            if (gitlabCommitToSprintMap.get(commitId) != null) {
                continue;
            }

            List<String> commitIssues = getIssueFromCommitMessage(commitMsg, jiraSprintMap.keySet());
            commitObj.put("related_issues", commitIssues);

            Set<String> sprintNames = extractSprintNames(commitObj, commitIssues);
            if (sprintNames.isEmpty()) {
                if (!shouldStopFetchingCommits()) {
                    stopFetchingCommits();
                }
                break;
            }

            filteredArray.put(commitObj);
            gitlabCommitToSprintMap.put(commitId, sprintNames);
        }

        tracer.trace("Page " + pageNumber + " Done. Total commits fetched after filtering: " + filteredArray.length());
        owner.decreaseNumberOfRunningFlow();
        return filteredArray;

    }

    private Set<String> extractSprintNames(JSONObject commitObj, List<String> commitIssues) throws JSONException {
        Set<String> commitSprintNames = new HashSet<>();
        String time = (String) commitObj.get("created_at");
        Date commitDate = DateUtils.parseDateToLocalTime(time);
        if (!CommonCollectorsUtils.isDateInsideDaysLimit(commitDate)) {
            return commitSprintNames;
        }

        List<String> potentialSprints = getPotentialSprints(commitDate, sprintsDatesMap);
        for (String commitIssue : commitIssues) {
            List<String> sprintNamesList = CommonCollectorsUtils.getSprintNames(commitIssue, potentialSprints,
                    jiraSprintMap);

            if (sprintNamesList != null){
                commitSprintNames.addAll(sprintNamesList);
            }
        }

        if (commitSprintNames.isEmpty()) {
            commitSprintNames.add("externalData");
        }

        return commitSprintNames;
    }

    private void addGitLabUserName(JSONObject commitObj, UserManager userManager) throws JSONException {
        String name = commitObj.getString("author_name");
        String email = commitObj.getString("author_email");
        boolean isSuccessfullyAdded = userManager.addToolToUserByEmailOrFullNameOrUserName("gitlab", name, email,
                name);
        if (isSuccessfullyAdded) {
            User snapglueUser = userManager.matchUserByEmailOrFullNameOrUserName(name,
                    email,
                    name);
            commitObj.put("snapglueUser", snapglueUser.getFullName());
        }
    }

}

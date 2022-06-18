package com.nigealm.agent.impl.jenkins;

import com.nigealm.agent.users.User;
import com.nigealm.agent.users.UserManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Created by Gil on 13/02/2016.
 * Class to retrieve Jenkins Builds
 */
public class JenkinsBuildRetrieverCallable extends AbstractJenkinsDataRetriever implements Callable<JSONArray> {

    private JSONObject buildToProcess;
    private UserManager userManager;

    public JenkinsBuildRetrieverCallable(JSONObject buildToProcess, UserManager userManager,
                                         String hostname, String port, String jobName, String user, String password) {
        super(hostname, port, jobName, user, password);
        this.buildToProcess = buildToProcess;
        this.userManager = userManager;
    }

    @Override
    public JSONArray call() throws Exception {
        String status = buildToProcess.getString("result");
        boolean isBuilding = buildToProcess.getBoolean("building");
        if (isBuilding || !("FAILURE".equalsIgnoreCase(status) ||
                "SUCCESS".equalsIgnoreCase(status) || "UNSTABLE".equalsIgnoreCase(status))) {
            return null;
        }

        buildToProcess.remove("fingerprint");
        return duplicateBuildPerExecutors();
    }


    private JSONArray duplicateBuildPerExecutors() throws JSONException {
        JSONArray returnedBuildsArray = new JSONArray();
        String jenkinsUserName;
        JSONObject changeSetObj = buildToProcess.getJSONObject("changeSet");
        JSONArray itemsArray = changeSetObj.getJSONArray("items");
        Set<String> buildUsers = new HashSet<>();
        boolean buildAdded = false;
        for (int i = 0; i < itemsArray.length(); i++) {
            jenkinsUserName = extractAuthorNameFromCurrChangeSet(itemsArray, i);
            boolean isSuccessfullyAdded = addExecutorToUsers(jenkinsUserName);
            boolean isNewUser  = isSuccessfullyAdded && buildUsers.add(jenkinsUserName);
            if (isNewUser){
                JSONObject newBuildObj = duplicateBuildPerUser(jenkinsUserName);
                returnedBuildsArray.put(newBuildObj);
                buildUsers.add(jenkinsUserName);
                buildAdded = true;
            }
        }

        if (!buildAdded){
            jenkinsUserName = "Anonymous";
            JSONObject newBuildObj = duplicateBuildPerUser(jenkinsUserName);
            returnedBuildsArray.put(newBuildObj);
            buildUsers.add(jenkinsUserName);
        }

        return returnedBuildsArray;
    }

    private JSONObject duplicateBuildPerUser(String jenkinsUserName) throws JSONException {
        JSONObject newBuildObj = new JSONObject(buildToProcess.toString());
        newBuildObj.put("executor", jenkinsUserName);
        User user = userManager.matchUserByEmailOrFullNameOrUserName(jenkinsUserName, jenkinsUserName,
                jenkinsUserName);
        if(user != null)
        	newBuildObj.put("snapglueUser", user.getFullName());
        return newBuildObj;
    }

    private String extractAuthorNameFromCurrChangeSet(JSONArray itemsArray, int i) throws JSONException {
        String jenkinsUserName;JSONObject currItem = itemsArray.getJSONObject(i);
        JSONObject authorObj = currItem.getJSONObject("author");
        jenkinsUserName = authorObj.getString("fullName");
        return jenkinsUserName;
    }

    private boolean addExecutorToUsers(String jenkinsUserName) throws JSONException {
        boolean isSuccessfullyAdded = false;
        if (jenkinsUserName != null && !"null".equals(jenkinsUserName)) {
            isSuccessfullyAdded = userManager.addToolToUserByEmailOrFullNameOrUserName("jenkins", jenkinsUserName,
                    jenkinsUserName,
                    jenkinsUserName);
        }
        return isSuccessfullyAdded;
    }
}

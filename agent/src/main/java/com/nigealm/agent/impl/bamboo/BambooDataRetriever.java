package com.nigealm.agent.impl.bamboo;

import com.nigealm.agent.impl.DataPerSprint;
import com.nigealm.agent.users.UserManager;
import com.nigealm.common.utils.Tracer;
import com.sun.jersey.core.util.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Gil on 19/07/2015.
 * <p/>
 * Class to retrieve Bamboo data
 */
public class BambooDataRetriever {
    protected static final String HEADER_AUTHORIZATION = "Authorization";
    protected static final String HEADER_ACCEPT = "Accept";
    private final static Tracer tracer = new Tracer(BambooDataRetriever.class);
    private String username;
    private String password;
    private String server;
    private String port;

    public BambooDataRetriever(String username, String password, String server, String port) {
        this.username = (username == null) ? "" : username;
        this.password = (password == null) ? "" : password;
        this.server = server;
        this.port = port;
    }

    public static void main(String args[]) throws JSONException, ParseException, IOException {
        BambooDataRetriever retriever = new BambooDataRetriever("gilams", "snapglue", "localhost", "8085");
        retriever.retrieveBambooData("SG", "DEF", null, true, new
                UserManager(), new ConcurrentHashMap<String, Set<String>>(), new ConcurrentHashMap<String,
                Set<String>>());
    }

    private String retrieveData(String customUrl) throws IOException {
        URL urlConnection;
        HttpURLConnection connection = null;

        try {
            urlConnection = new URL(customUrl);
            connection = (HttpURLConnection) urlConnection.openConnection();
            connection.setRequestMethod("GET");
            if (!username.isEmpty() && !password.isEmpty()) {
                connection.setRequestProperty(HEADER_AUTHORIZATION, "Basic " + Arrays.toString(Base64.encode(username
                        + ":"
                        + password)));
                connection.setRequestProperty(HEADER_ACCEPT, "application/json");
            }

            if (connection.getResponseCode() != 200) {
                tracer.warn("Failed : HTTP error code : "
                        + connection.getResponseCode());
                return "";
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (connection.getInputStream())));

            String output;
            StringBuilder buildData = new StringBuilder();
            while ((output = br.readLine()) != null) {
                buildData.append(output);
            }
            return buildData.toString();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public DataPerSprint retrieveBambooData(String projectKey, String planKey, Date lastIterationTime, boolean
            retrieveAllData, UserManager userManager, Map<String, Set<String>> githubCommitToSprintMap,
                                            Map<String, Set<String>> gitlabCommitToSprintMap) throws
            IOException, JSONException, ParseException {
        retrieveAllData = retrieveAllData || lastIterationTime == null;
        DataPerSprint dataPerSprint = new DataPerSprint();
        String customUrl = "http://" + server + ":" + port + "/rest/api/latest/result/" + projectKey + "-" + planKey
                + ".json";
        String jsonStr = retrieveData(customUrl);

        JSONArray jsonTempArray = getTempJSONArray(jsonStr);
        for (int i = 0; i < jsonTempArray.length(); i++) {
            JSONObject currBuild = getBuildDataJsonObject(jsonTempArray, i);
            String buildCompletedDateStr = currBuild.getString("buildCompletedDate");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            Date buildCompletedDate = dateFormat.parse(buildCompletedDateStr);
            if (retrieveAllData || buildCompletedDate.after(lastIterationTime)) {
                addBambooUser(currBuild, userManager);
                Set<String> sprintNamesSet = extractSprintNames(currBuild, githubCommitToSprintMap,
                        gitlabCommitToSprintMap);
                for (String sprintName : sprintNamesSet) {
                    dataPerSprint.addData(sprintName, currBuild);
                }
            }
        }

        return dataPerSprint;
    }

    private Set<String> extractSprintNames(JSONObject buildObj, Map<String, Set<String>>
            githubCommitToSprintMap, Map<String, Set<String>> gitlabCommitToSprintMap) {
        Set<String> sprintNamesList = new HashSet<>();
        try {
            JSONObject changesObj = buildObj.getJSONObject("changes");
            JSONArray changesArray = changesObj.getJSONArray("change");
            for (int i = 0; i < changesArray.length(); i++) {
                JSONObject currChange = changesArray.getJSONObject(i);
                String commitId = currChange.getString("changesetId");
                Set<String> sprintsNames = githubCommitToSprintMap.get(commitId);
                if (sprintsNames == null) {
                    sprintsNames = gitlabCommitToSprintMap.get(commitId);
                }
//                if (sprintsNames == null) {
//                    sprintsNames = bitbucketCommitToSprintMap.get(commitId);
//                }
                if (sprintsNames != null) {
                    sprintNamesList.addAll(sprintsNames);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (sprintNamesList.isEmpty()){
            sprintNamesList.add("externalData");
        }

        return sprintNamesList;
    }

    private JSONArray getTempJSONArray(String jsonStr) {
        JSONArray arrayToReturn = new JSONArray();
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            jsonObj = (JSONObject) jsonObj.get("results");
            Object resultObj = jsonObj.get("result");
            if (resultObj instanceof JSONObject) {
                arrayToReturn.put(resultObj);
            }
            if (resultObj instanceof JSONArray) {
                arrayToReturn = (JSONArray) resultObj;
            }
        } catch (JSONException e) {
            tracer.exception("getTempJSONArray", e);
        }
        return arrayToReturn;

    }

    private JSONObject getBuildDataJsonObject(JSONArray jsonTempArray, int i) throws JSONException, IOException {
        JSONObject currObj = (JSONObject) jsonTempArray.get(i);
        String buildResultKey = currObj.getString("buildResultKey");
        String buildUrl = "http://" + server + ":" + port + "/rest/api/latest/result/" + buildResultKey +
                ".json?expand=changes,metadata,vcsRevisions,artifacts,comments,labels,jiraIssues,stages";
        String buildJson = retrieveData(buildUrl);
        if (buildJson == null) {
            return new JSONObject();
        }
        return new JSONObject(buildJson);
    }

    private void addBambooUser(JSONObject buildObject, UserManager userManager) {
        try {
            JSONObject changesObj = buildObject.getJSONObject("changes");
            JSONArray changeArray = changesObj.getJSONArray("change");
            for (int i = 0; i < changeArray.length(); i++) {
                JSONObject currChangeObj = changeArray.getJSONObject(i);
                String authorStr = currChangeObj.getString("author");
                String fullName = authorStr.substring(0, authorStr.indexOf('<')).trim();
                String email = authorStr.substring(authorStr.indexOf('<') + 1, authorStr.indexOf('>'));
                boolean isUserAddedSuccessfully = userManager.addToolToUserByEmailOrFullNameOrUserName("bamboo",
                        null, email, fullName);
                if (!isUserAddedSuccessfully) {
                    tracer.warn("User wasn't added for build: " + buildObject.getInt("buildNumber"));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}

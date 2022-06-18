package com.nigealm.agent.impl.bitbucket;

import com.nigealm.agent.impl.CommonCollectorsUtils;
import com.nigealm.agent.impl.DataPerSprint;
import com.nigealm.agent.impl.JsonUtils;
import com.nigealm.agent.users.User;
import com.nigealm.agent.users.UserManager;
import com.nigealm.common.utils.DateUtils;
import com.nigealm.common.utils.Tracer;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.*;
import org.apache.http.auth.*;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
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

public class BitbucketDataRetriever {

    private static final Tracer tracer = new Tracer(BitbucketDataRetriever.class);
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String BASE_URL = "https://api.bitbucket.org/2.0/repositories";
    private String accessToken;

    public BitbucketDataRetriever(String accessToken) {
        this.accessToken = accessToken;
    }

    public static void main(String args[]) throws JSONException, IOException {
        BitbucketDataRetriever retriever = new BitbucketDataRetriever
                ("");
        retriever.getCommits("Equalum/agent", new Date(0), new UserManager(), new
                        ConcurrentHashMap<String, Set< String>>(), new ConcurrentHashMap<String, Set<String>>(),
                new HashMap<String, Pair<Date, Date>>());
    }

    public DataPerSprint getCommits(String repoName, Date lastIterationDate, UserManager
            userManager, Map<String, Set<String>> jiraToSprintMap, Map<String, Set<String>>
                                            bitbucketCommitToSprintMap, Map<String, Pair<Date, Date>>
                                            sprintsDatesMap) throws
            JSONException, IOException {
        DataPerSprint dataPerSprint = new DataPerSprint();
        JSONArray commitsArray = new JSONArray();
        JSONArray currCommitsArray = buildBitbucketCommitToSprintMapAndGetCommitsArray(repoName,
                jiraToSprintMap, bitbucketCommitToSprintMap, sprintsDatesMap);
        currCommitsArray = filterCommitsByLastIterationDate(currCommitsArray, lastIterationDate);
        addBitbucketUsers(currCommitsArray, userManager);
        JsonUtils.mergeJSONArrays(commitsArray, currCommitsArray);

        splitCommitsBySprint(bitbucketCommitToSprintMap, dataPerSprint, commitsArray);
        return dataPerSprint;
    }

    private JSONArray buildBitbucketCommitToSprintMapAndGetCommitsArray(String repoName, Map<String, Set<String>>
            jiraToSprintMap, Map<String, Set<String>> bitbucketCommitToSprintMap, Map<String, Pair<Date,
            Date>> sprintsDatesMap) throws JSONException,
            IOException {
        String customUrl = BASE_URL + "/" + repoName + "/commits?pagelen=100";
        JSONArray commitsArray = getCommitsArray(customUrl);
        for (int i = 0; i < commitsArray.length(); i++) {
            JSONObject currCommitObj = commitsArray.getJSONObject(i);
            String commitId = currCommitObj.getString("hash");
            String commitMsg = currCommitObj.getString("message");
            List<String> commitIssues = getIssueFromCommitMessage(commitMsg, jiraToSprintMap.keySet());
            currCommitObj.put("related_issues", commitIssues);
            Set<String> sprintName = extractSprintsNames(jiraToSprintMap, sprintsDatesMap, currCommitObj,
                    commitIssues);
            bitbucketCommitToSprintMap.put(commitId, sprintName);
        }
        return commitsArray;
    }

    private Set<String> extractSprintsNames(Map<String, Set<String>> jiraToSprintMap, Map<String, Pair<Date, Date>>
            sprintsDatesMap, JSONObject currCommitObj, List<String> commitIssues) throws JSONException {
        Set<String> sprintsNames = new HashSet<>();
        Date commitDate = DateUtils.parseDateToLocalTime(currCommitObj.getString("date"));
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

    private void splitCommitsBySprint(Map<String, Set<String>> bitbucketCommitToSprintMap, DataPerSprint
            dataPerSprint, JSONArray commitsArray) {
        for (int i = 0; i < commitsArray.length(); i++) {
            try {
                JSONObject currCommitObj = commitsArray.getJSONObject(i);
                String commitId = currCommitObj.getString("hash");
                Set<String> sprintsNames = bitbucketCommitToSprintMap.get(commitId);
                for (String sprintName : sprintsNames) {
                    dataPerSprint.addData(sprintName, currCommitObj);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private JSONArray getCommitsArray(String customUrl) throws JSONException, IOException {
        JSONArray commitsArray = new JSONArray();
        String nextUrl = customUrl;
        boolean shouldStop = false;
        while (nextUrl != null && !shouldStop) {
            String commitsJson = retrieveData(nextUrl, 3);
//            String commitsJson = retrieveBitbucketDataWithAuth(nextUrl);
            if (commitsJson == null) {
                nextUrl = null;
                continue;
            }
            JSONObject responseObj = new JSONObject(commitsJson);
            JSONArray currCommits = responseObj.getJSONArray("values");
            shouldStop = addCurrCommitToCommitsArray(currCommits, commitsArray);
            nextUrl = getNextUrl(responseObj);
        }

        return commitsArray;
    }

    private String getNextUrl(JSONObject responseObj) throws JSONException {
        return (responseObj.has("next")) ? responseObj.getString("next") : null;
    }

    private JSONArray filterCommitsByLastIterationDate(JSONArray commitsArray, Date lastIterationDate) throws
            JSONException {
        if (lastIterationDate == null) {
            return commitsArray;
        }

        JSONArray filteredArray = new JSONArray();
        for (int i = 0; i < commitsArray.length(); i++) {
            JSONObject currCommit = commitsArray.getJSONObject(i);
            Date commitDate = DateUtils.parseDateToLocalTime(currCommit.getString("date"));
            if (commitDate != null && commitDate.after(lastIterationDate)) {
                filteredArray.put(currCommit);
            }
        }
        return filteredArray;
    }


    private boolean addCurrCommitToCommitsArray(JSONArray currCommits, JSONArray commitsArray)
            throws JSONException {
        boolean shouldStopFetchCommits = false;
        try {
            for (int i = 0; i < currCommits.length(); i++) {
                JSONObject currCommit = currCommits.getJSONObject(i);
                String commitMsg = currCommit.getString("message");
                if (isMergeCommit(commitMsg)) {
                    continue;
                }

                Date currCommitDate = DateUtils.parseDateToLocalTime(currCommit.getString("date"));
                if (isDateInsideDaysLimit(currCommitDate)) {
                    commitsArray.put(currCommit);
                } else {
                    shouldStopFetchCommits = true;
                    break;
                }
            }
        } catch (Exception e) {
            tracer.exception("addCurrCommitToCommitsArray", e);
        }
        return shouldStopFetchCommits;
    }

    private void addBitbucketUsers(JSONArray commitsArray, UserManager userManager) {
        for (int i = 0; i < commitsArray.length(); i++) {
            try {
                String fullName = "";
                String userName = "";
                String email = "";
                JSONObject currCommit = (JSONObject) commitsArray.get(i);
                JSONObject authorObject = currCommit.getJSONObject("author");
                JSONObject userObject = null;

                if (currCommit.has("user")) {
                    userObject = currCommit.getJSONObject("user");
                }

                if (authorObject != null && authorObject.has("user")) {
                    userObject = authorObject.getJSONObject("user");
                    email = extractEmailFromRaw(authorObject.getString("raw"));
                }

                if (userObject != null) {
                    fullName = userObject.getString("display_name");
                    userName = userObject.getString("username");
                }

                boolean isUserAddedSuccessfully = userManager.addToolToUserByEmailOrFullNameOrUserName("bitbucket",
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

    private String extractEmailFromRaw(String raw) {
        int startIndex = raw.indexOf('<') + 1;
        int endIndex = raw.indexOf('>');
        return raw.substring(startIndex, endIndex);
    }

    private String retrieveData(String customUrl, int retries) throws IOException, JSONException {
        URL urlConnection;
        HttpURLConnection connection = null;

        try {
            urlConnection = new URL(customUrl);
            connection = (HttpURLConnection) urlConnection.openConnection();
            connection.setRequestMethod("GET");
            if (!isEmpty(accessToken)) {
                connection.setRequestProperty(HEADER_AUTHORIZATION, "Bearer " + accessToken);
            } else {
                tracer.warn("Authentication data is empty for Bitbucket");
            }

            if (connection.getResponseCode() != 200) {
                if (retries > 0) {
                    waitBeforeRetry();
                    return retrieveData(customUrl, --retries);
                } else {
                    tracer.warn("Failed : HTTP error code : "
                            + connection.getResponseCode());
                    return null;
                }
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (connection.getInputStream())));

            String output;
            StringBuilder commits = new StringBuilder();
            while ((output = br.readLine()) != null) {
                commits.append(output);
            }
            return commits.toString();
        } finally {
            if (connection != null)
                connection.disconnect();
        }
    }

    private void waitBeforeRetry() {
        try {
            Thread.sleep(120000);
        } catch (InterruptedException e1) {
            Thread.currentThread().interrupt();
            tracer.exception("BitbucketDataRetriever#waitBeforeRetry", e1);
        }
    }

    private String retrieveBitbucketDataWithAuth(String url) {
        try {
            DefaultHttpClient client = new DefaultHttpClient();
            client.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                    new UsernamePasswordCredentials("aviam12", "avshi1212!!"));
            BasicScheme basicAuth = new BasicScheme();
            BasicHttpContext context = new BasicHttpContext();
            context.setAttribute("preemptive-auth", basicAuth);
            client.addRequestInterceptor(new PreemptiveAuth(), 0);
            HttpGet get = new HttpGet(url);
            HttpResponse response = client.execute(get, context);

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                System.out.println(statusCode);
            }

            HttpEntity entity = response.getEntity();
            return EntityUtils.toString(entity);

        } catch (Exception e) {
            tracer.exception("retrieveBitbucketDataWithAuth", e);
            return null;
        }
    }

    protected static class PreemptiveAuth implements HttpRequestInterceptor {
        public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
            // Get the AuthState
            AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);

            // If no auth scheme available yet, try to initialize it
            if (authState.getAuthScheme() == null) {
                AuthScheme authScheme = (AuthScheme) context.getAttribute("preemptive-auth");
                CredentialsProvider credProvider = (CredentialsProvider) context
                        .getAttribute(ClientContext.CREDS_PROVIDER);
                HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
                if (authScheme != null) {
                    Credentials cred = credProvider.getCredentials(new AuthScope(targetHost.getHostName(), targetHost
                            .getPort()));
                    if (cred == null) {
                        throw new HttpException("No credentials for preemptive authentication");
                    }
                    authState.update(authScheme, cred);
                }
            }
        }
    }


}

package com.nigealm.agent.impl.gitlab;

import com.nigealm.agent.impl.AgentDataCollectionException;
import com.nigealm.common.utils.Tracer;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractGitLabDataRetriever {

    private static final Object syncObj = new Object();

    protected final static Tracer tracer = new Tracer(AbstractGitLabDataRetriever.class);

    protected final static Map<String, AtomicBoolean> stopFetchingCommitsMap = new ConcurrentHashMap<>();

    protected final static Map<String, Object> syncObjectsMap = new ConcurrentHashMap<>();

    protected int numberOfRunningRequests;

    protected String baseUrl;

    protected String projectName;

    protected String token;

    protected String confId;


    public AbstractGitLabDataRetriever(String confId, String token, String baseUrl, String projectName) {
        this.token = token;
        this.baseUrl = baseUrl;
        this.projectName = projectName;
        this.confId = confId;
    }

    protected void initSyncAndStopObjects() {
        synchronized (syncObj) {
            initStopFetchingCommitFlag();
            initSyncObject();
        }
    }

    private void initSyncObject() {
        if (syncObjectsMap.get(confId) == null) {
            syncObjectsMap.put(confId, new Object());
        }
    }

    private void initStopFetchingCommitFlag() {
        if (stopFetchingCommitsMap.get(confId) == null) {
            stopFetchingCommitsMap.put(confId, new AtomicBoolean(false));
        }
    }

    protected boolean shouldStopFetchingCommits() {
        return stopFetchingCommitsMap.get(confId).get();
    }

    protected void stopFetchingCommits() {
        stopFetchingCommitsMap.get(confId).set(true);
    }

    protected String retrieveData(String customUrl) throws AgentDataCollectionException {
        return retrieveData(customUrl, 3);
    }

    private String retrieveData(String customUrl, int retries) throws AgentDataCollectionException {
        HttpURLConnection connection = null;
        try {
            connection = getConnectionWithPrivateToken(customUrl);

            if (connection.getResponseCode() != 200) {
                if (retries > 0) {
                    waitBeforeRetry();
                    return retrieveData(customUrl, --retries);
                }
                tracer.warn(confId + ": GitLab Failed to fetch data from: " + customUrl + ",  HTTP error code : "
                        + connection.getResponseCode());
                throw new AgentDataCollectionException(confId, "gitlab");
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (connection.getInputStream())));

            String output;
            StringBuilder commits = new StringBuilder();
            while ((output = br.readLine()) != null) {
                commits.append(output);
            }
            return commits.toString();
        } catch (IOException e) {
            if (retries > 0) {
                waitBeforeRetry();
                return retrieveData(customUrl, --retries);
            }
            throw new AgentDataCollectionException(confId, "gitlab", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void waitBeforeRetry() {
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e1) {
            Thread.currentThread().interrupt();
            tracer.exception("GitLabCommitsFetcherCallable#call", e1);
        }
    }

    private HttpURLConnection getConnectionWithPrivateToken(String customUrl) throws IOException {
        // Add the correct character to append the token var in the url
        if (customUrl.contains("?")) {
            customUrl += "&";
        } else {
            customUrl += "?";
        }

        customUrl += "private_token=" + token;
        URL urlConnection = new URL(customUrl);
        HttpURLConnection connection = (HttpURLConnection) urlConnection.openConnection();
        connection.setRequestMethod("GET");
        return connection;
    }

    public void cleanup() throws JSONException {
        syncObjectsMap.remove(confId);
        stopFetchingCommitsMap.remove(confId);
    }

}

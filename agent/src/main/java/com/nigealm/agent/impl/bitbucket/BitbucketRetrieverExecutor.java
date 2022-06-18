package com.nigealm.agent.impl.bitbucket;

import com.mongodb.client.FindIterable;
import com.nigealm.agent.impl.DataPerSprint;
import com.nigealm.agent.svc.MongoDBAgentServiceImpl;
import com.nigealm.agent.users.UserManager;
import com.nigealm.common.utils.Tracer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;

import static com.nigealm.agent.impl.MongoDBConstants.*;

public class BitbucketRetrieverExecutor implements Callable<DataPerSprint> {
    private final static Tracer tracer = new Tracer(BitbucketRetrieverExecutor.class);
    private String confId;
    private Document doc;
    private Date lastIterationTime;
    private Map<String, Pair<Date, Date>> sprintsDatesMap;
    private Map<String, Set<String>> bitbucketCommitToSprintMap;
    private Map<String, Set<String>> jiraToSprintMap;
    private UserManager userManager;

    public BitbucketRetrieverExecutor(String confId, Document doc, Date lastIterationTime,
                                      Map<String, Pair<Date, Date>> sprintsDatesMap,
                                      Map<String, Set<String>> bitbucketCommitToSprintMap,
                                      Map<String, Set<String>> jiraToSprintMap, UserManager userManager) {
        this.confId = confId;
        this.doc = doc;
        this.lastIterationTime = lastIterationTime;
        this.sprintsDatesMap = sprintsDatesMap;
        this.bitbucketCommitToSprintMap = bitbucketCommitToSprintMap;
        this.jiraToSprintMap = jiraToSprintMap;
        this.userManager = userManager;
    }

    @Override
    public DataPerSprint call() throws Exception {
        org.bson.Document bitbucketDoc = (org.bson.Document) doc.get(MONGODB_CONFIGURATION_TOOL_NAME_BITBUCKET);
        if (bitbucketDoc == null || bitbucketDoc.isEmpty())
            return null;
        String refreshToken = bitbucketDoc.getString(MONGODB_CONFIGURATION_BITBUCKET_REFRESH_TOKEN);
        String repositoryNames = bitbucketDoc.getString(MONGODB_CONFIGURATION_BITBUCKET_REPOSITORY_NAME);
        if (StringUtils.isEmpty(repositoryNames)){
            tracer.info("Bitbucket repo is empty. skipping Bitbucket");
            return null;
        }

        tracer.trace("-------------Bitbucket-------------");
        String accessToken = getNewAccessToken(refreshToken);
        BitbucketDataRetriever bitbucketDataRetriever = new BitbucketDataRetriever(accessToken);
        DataPerSprint dataPerSprint = new DataPerSprint();
        String[] repoNameArray = repositoryNames.split(",");
        if (repoNameArray == null || repoNameArray.length == 0){
            tracer.info("Bitbucket repo is failed to split. skipping Bitbucket");
            return null;
        }
        for (String currRepoName : repoNameArray) {

            DataPerSprint currRepoData = bitbucketDataRetriever.getCommits(currRepoName.trim(), lastIterationTime,
                    userManager, jiraToSprintMap, bitbucketCommitToSprintMap, sprintsDatesMap);
            dataPerSprint.mergeData(currRepoData);
        }
        return  dataPerSprint;
    }

    private String getNewAccessToken(String refreshToken) throws JSONException, IOException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        String url = "https://bitbucket.org/site/oauth2/access_token";
        HttpPost httpPost = new HttpPost(url);
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("client_id", "VZxR4pu5evmHpQC9pv"));
        nvps.add(new BasicNameValuePair("client_secret", "L7qAPjAQr4A5M2uDMGjuUe5zDE95RRmb"));
        nvps.add(new BasicNameValuePair("grant_type", "refresh_token"));
        nvps.add(new BasicNameValuePair("refresh_token", refreshToken));
        httpPost.setEntity(new UrlEncodedFormEntity(nvps, Consts.UTF_8));
        HttpResponse response = httpClient.execute(httpPost);
        String responseJson = EntityUtils.toString(response.getEntity());
        return extractTokensAndSaveToDB(responseJson);
    }

    private String extractTokensAndSaveToDB(String responseJson) throws JSONException {
        JSONObject responseObj = new JSONObject(responseJson);
        String oauthToken = responseObj.getString("access_token");
        String refreshToken = responseObj.getString("refresh_token");
        tracer.trace("Bitbucket new token isEmpty: " + StringUtils.isEmpty(oauthToken));
        tracer.trace("Bitbucket new refresh token isEmpty: " + StringUtils.isEmpty(refreshToken));
        MongoDBAgentServiceImpl mongoService = new MongoDBAgentServiceImpl();
        FindIterable<Document> allDocuments = mongoService.getAllDocuments();
        for (org.bson.Document document : allDocuments) {
            String currentConfId = document.get("_id").toString();
            if (!confId.equals(currentConfId)) {
                continue;
            }

            org.bson.Document doc = (org.bson.Document) document.get("tools");
            org.bson.Document bitbucketDoc = (org.bson.Document) doc.get(MONGODB_CONFIGURATION_TOOL_NAME_BITBUCKET);
            bitbucketDoc.put(MONGODB_CONFIGURATION_BITBUCKET_REFRESH_TOKEN, refreshToken);
            mongoService.updateConfiguration(document, confId);
        }

        return (!StringUtils.isEmpty(oauthToken)) ? oauthToken : "";
    }
}

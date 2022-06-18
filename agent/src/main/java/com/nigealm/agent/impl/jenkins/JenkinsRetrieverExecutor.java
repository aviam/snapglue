package com.nigealm.agent.impl.jenkins;

import com.nigealm.agent.impl.DataPerSprint;
import com.nigealm.agent.users.UserManager;
import com.nigealm.common.utils.Tracer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.Document;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.Callable;

import static com.nigealm.agent.impl.MongoDBConstants.*;

/**
 * Created by Gil on 12/02/2016.
 * Class to trigger Jenkins builds retrievers
 */
public class JenkinsRetrieverExecutor implements Callable<DataPerSprint> {
    private final static Tracer tracer = new Tracer(JenkinsRetrieverExecutor.class);
    private Document doc;
    private boolean retrieveAllData;
    private int lastBuildNumber;
    private Map<String, Pair<Date, Date>> sprintsDatesMap;
    private UserManager userManager;
    private Map<String, Set<String>> githubCommitToSprintMap;
    private Map<String, Set<String>> gitlabCommitToSprintMap;
    private Map<String, Set<String>> bitbucketCommitToSprintMap;
    private HashMap<String, JSONObject> commitIdToCommitObjectMap;

    public JenkinsRetrieverExecutor(Document doc, boolean retrieveAllData, int lastBuildNumber, Map<String,
            Pair<Date, Date>> sprintsDatesMap, UserManager userManager,
                                    Map<String, Set<String>> githubCommitToSprintMap, Map<String, Set<String>>
                                    gitlabCommitToSprintMap, Map<String, Set<String>> bitbucketCommitToSprintMap,
                                    HashMap<String, JSONObject> commitIdToCommitObjectMap) {
        this.doc = doc;
        this.retrieveAllData = retrieveAllData;
        this.lastBuildNumber = lastBuildNumber;
        this.sprintsDatesMap = sprintsDatesMap;
        this.userManager = userManager;
        this.githubCommitToSprintMap = githubCommitToSprintMap;
        this.gitlabCommitToSprintMap = gitlabCommitToSprintMap;
        this.bitbucketCommitToSprintMap = bitbucketCommitToSprintMap;
        this.commitIdToCommitObjectMap = commitIdToCommitObjectMap;
    }

    @Override
    public DataPerSprint call() throws Exception {
        org.bson.Document jenkinsDoc = (org.bson.Document) doc.get(MONGODB_CONFIGURATION_TOOL_NAME_JENKINS);
        if (jenkinsDoc == null || jenkinsDoc.isEmpty())
            return null;

        String hostname = (String) jenkinsDoc.get(MONGODB_CONFIGURATION_JENKINS_HOSTNAME);
        String port = (String) jenkinsDoc.get(MONGODB_CONFIGURATION_JENKINS_PORT);
        String jobNames = (String) jenkinsDoc.get(MONGODB_CONFIGURATION_JENKINS_PROJECT_NAME);
        String user = (String) jenkinsDoc.get(MONGODB_CONFIGURATION_JENKINS_USER);
        String password = (String) jenkinsDoc.get(MONGODB_CONFIGURATION_JENKINS_PASSWORD);

        if (StringUtils.isEmpty(hostname)){
            tracer.info("Jenkins repo is empty. skipping Jenkins");
            return null;
        }

        tracer.trace("-------------Jenkins-------------");
        String[] jobNamesArray = jobNames.split(",");
        JenkinsBuildsRetriever jenkinsBuildsDataRetriever;
        DataPerSprint jenkinsBuildData = new DataPerSprint();
        for (String jobname : jobNamesArray) {
            DataPerSprint currBuildData;
            if (user == null || user.isEmpty()) {
                jenkinsBuildsDataRetriever = new JenkinsBuildsRetriever(hostname, port, jobname.trim());
                currBuildData = jenkinsBuildsDataRetriever.retrieveJenkinsBuilds(retrieveAllData, lastBuildNumber,
                        userManager, githubCommitToSprintMap, gitlabCommitToSprintMap, bitbucketCommitToSprintMap,
                        commitIdToCommitObjectMap, sprintsDatesMap);
            } else {
                jenkinsBuildsDataRetriever = new JenkinsBuildsRetriever(hostname, port, jobname.trim(), user, password);
                currBuildData = jenkinsBuildsDataRetriever.retrieveJenkinsBuildsWithAuth(retrieveAllData,
                        lastBuildNumber, userManager, githubCommitToSprintMap, gitlabCommitToSprintMap,
                        bitbucketCommitToSprintMap, commitIdToCommitObjectMap, sprintsDatesMap);
            }
            jenkinsBuildData.mergeData(currBuildData);
        }
        return jenkinsBuildData;

    }
}

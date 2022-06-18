package com.nigealm.agent.impl.bamboo;

import com.nigealm.agent.impl.DataPerSprint;
import com.nigealm.agent.users.UserManager;
import com.nigealm.common.utils.Tracer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.Document;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import static com.nigealm.agent.impl.MongoDBConstants.*;

public class BambooRetrieverExecutor implements Callable<DataPerSprint> {
    private final static Tracer tracer = new Tracer(BambooRetrieverExecutor.class);
    private Document doc;
    private boolean retrieveAllData;
    private Date lastIterationTime;
    private Map<String, Pair<Date, Date>> sprintsDatesMap;
    private UserManager userManager;
    private Map<String, Set<String>> githubCommitToSprintMap;
    private Map<String, Set<String>> gitlabCommitToSprintMap;
    private Map<String, Set<String>> bitbucketCommitToSprintMap;

    public BambooRetrieverExecutor(Document doc, boolean retrieveAllData, Date lastIterationTime, Map<String,
            Pair<Date, Date>> sprintsDatesMap, UserManager
            userManager, Map<String, Set<String>> githubCommitToSprintMap,
                                   Map<String, Set<String>> gitlabCommitToSprintMap,
                                   Map<String, Set<String>> bitbucketCommitToSprintMap) {
        this.doc = doc;
        this.retrieveAllData = retrieveAllData;
        this.lastIterationTime = lastIterationTime;
        this.sprintsDatesMap = sprintsDatesMap;
        this.userManager = userManager;
        this.githubCommitToSprintMap = githubCommitToSprintMap;
        this.gitlabCommitToSprintMap = gitlabCommitToSprintMap;
        this.bitbucketCommitToSprintMap = bitbucketCommitToSprintMap;
    }

    @Override
    public DataPerSprint call() throws Exception {
        org.bson.Document bambooDoc = (org.bson.Document) doc.get(MONGODB_CONFIGURATION_TOOL_NAME_BAMBOO);
        if (bambooDoc == null || bambooDoc.isEmpty())
            return null;

        String serverHost = (String) bambooDoc.get(MONGODB_CONFIGURATION_BAMBOO_HOSTNAME);
        String serverPort = (String) bambooDoc.get(MONGODB_CONFIGURATION_BAMBOO_PORT);
        String projectKey = (String) bambooDoc.get(MONGODB_CONFIGURATION_BAMBOO_PROJECT_KEY);
        String planKey = (String) bambooDoc.get(MONGODB_CONFIGURATION_BAMBOO_PLAN_KEY);
        String username = (String) bambooDoc.get(MONGODB_CONFIGURATION_BAMBOO_USER_NAME);
        String password = (String) bambooDoc.get(MONGODB_CONFIGURATION_BAMBOO_PASSWORD);

        if (StringUtils.isEmpty(serverHost)) {
            tracer.info("Bamboo repo is empty. skipping Bamboo");
            return null;
        }

        tracer.trace("-------------Bamboo-------------");
        BambooDataRetriever dataRetriever = new BambooDataRetriever(username, password, serverHost, serverPort);
        DataPerSprint bambooData = dataRetriever.retrieveBambooData(projectKey, planKey, lastIterationTime,
                retrieveAllData, userManager, githubCommitToSprintMap, gitlabCommitToSprintMap);
        return bambooData;
    }
}

package com.nigealm.agent.impl.github;

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

/**
 * Created by Gil on 12/02/2016.
 * Class to trigger GitHub Data retrievers
 */
public class GitHubRetrieverExecutor implements Callable<DataPerSprint> {
    private final static Tracer tracer = new Tracer(GitHubRetrieverExecutor.class);
    private Document doc;
    private Date lastIterationTime;
    private Map<String, Pair<Date, Date>> sprintsDatesMap;
    private Map<String, Set<String>> githubCommitToSprintMap;
    private Map<String, Set<String>> jiraToSprintMap;
    private UserManager userManager;

    public GitHubRetrieverExecutor(Document doc, Date lastIterationTime,
                                   Map<String, Pair<Date, Date>> sprintsDatesMap,
                                   Map<String, Set<String>> githubCommitToSprintMap,
                                   Map<String, Set<String>> jiraToSprintMap, UserManager userManager) {
        this.doc = doc;
        this.lastIterationTime = lastIterationTime;
        this.sprintsDatesMap = sprintsDatesMap;
        this.githubCommitToSprintMap = githubCommitToSprintMap;
        this.jiraToSprintMap = jiraToSprintMap;
        this.userManager = userManager;
    }

    @Override
    public DataPerSprint call() throws Exception {
        org.bson.Document githubDoc = (org.bson.Document) doc.get(MONGODB_CONFIGURATION_TOOL_NAME_GITHUB);
        if (githubDoc == null || githubDoc.isEmpty())
            return null;
        String username = githubDoc.getString(MONGODB_CONFIGURATION_GITHUB_USER_NAME);
        String password = githubDoc.getString(MONGODB_CONFIGURATION_GITHUB_PASSWORD);
        String token = githubDoc.getString(MONGODB_CONFIGURATION_GITHUB_TOKEN);
        String repositoryName = githubDoc.getString(MONGODB_CONFIGURATION_GITHUB_REPOSITORY_NAME);

        if (StringUtils.isEmpty(repositoryName)){
            tracer.info("GitHub repo is empty. skipping GitHub");
            return null;
        }

        tracer.trace("-------------GitHub-------------");
        GitHubDataRetriever gitHubDataRetriever =  new GitHubDataRetriever(username, password, token);
        return gitHubDataRetriever.getCommitsSince(repositoryName,
                    lastIterationTime, userManager, jiraToSprintMap, githubCommitToSprintMap, sprintsDatesMap);

    }
}

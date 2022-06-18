package com.nigealm.agent.impl.gitlab;

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
 * Class to trigger GitLab data retrievers
 */
public class GitLabRetrieverExecutor implements Callable<DataPerSprint> {
    private final static Tracer tracer = new Tracer(GitLabRetrieverExecutor.class);
    private Document doc;
    private Date lastIterationTime;
    private Map<String, Pair<Date, Date>> sprintsDatesMap;
    private Map<String, Set<String>> gitlabCommitToSprintMap;
    private Map<String, Set<String>> jiraToSprintMap;
    private UserManager userManager;
    private String confId;

    public GitLabRetrieverExecutor(String confId, Document doc, Date lastIterationTime, Map<String, Pair<Date, Date>>
            sprintsDatesMap, Map<String, Set<String>> gitlabCommitToSprintMap,
                                   Map<String, Set<String>> jiraToSprintMap, UserManager userManager) {
        this.doc = doc;
        this.lastIterationTime = lastIterationTime;
        this.sprintsDatesMap = sprintsDatesMap;
        this.gitlabCommitToSprintMap = gitlabCommitToSprintMap;
        this.jiraToSprintMap = jiraToSprintMap;
        this.userManager = userManager;
        this.confId = confId;
    }

    @Override
    public DataPerSprint call() throws Exception {

        org.bson.Document gitlabDoc = (org.bson.Document) doc.get(MONGODB_CONFIGURATION_TOOL_NAME_GITLAB);
        if (gitlabDoc == null || gitlabDoc.isEmpty())
            return null;

        String serverUrl = (String) gitlabDoc.get(MONGODB_CONFIGURATION_GITLAB_SERVER_URL);
        String token = (String) gitlabDoc.get(MONGODB_CONFIGURATION_GITLAB_TOKEN);
        String projectName = (String) gitlabDoc.get(MONGODB_CONFIGURATION_GITLAB_PROJECT_NAME);

        if (StringUtils.isEmpty(projectName)){
            tracer.info("GitLab repo is empty. skipping GitLab");
            return null;
        }

        tracer.trace("-------------GitLab-------------");
        GitLabDataRetriever gitLabDataRetriever = new GitLabDataRetriever(confId, serverUrl, token, projectName);
        return gitLabDataRetriever.getCommits(lastIterationTime, sprintsDatesMap,
                gitlabCommitToSprintMap, jiraToSprintMap, userManager);
    }
}

package com.nigealm.agent.impl.jira;

import com.nigealm.agent.impl.AgentDataCollectionException;
import com.nigealm.agent.users.UserManager;
import com.nigealm.common.utils.Tracer;
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

import java.io.IOException;

/**
 * Created by Gil on 12/02/2016.
 * Parent class for Jira retrievers
 */
public abstract class AbstractJiraDataRetriever {

    protected final static Tracer tracer = new Tracer(AbstractJiraDataRetriever.class);

    protected String projectName;

    protected String jiraServerUri;

    protected String username;

    protected String password;

    protected UserManager userManager;

    public AbstractJiraDataRetriever(String projectName, String jiraServerUri, String username, String password,
                                     UserManager userManager){
        this.projectName = projectName;
        this.jiraServerUri = jiraServerUri;
        this.username = username;
        this.password = password;
        this.userManager = userManager;
    }

    protected String retrieveData(String url) throws JiraRetrieveDataException, IOException {
        return retrieveData(url, 3);
    }

    private String retrieveData(String url, int retries) throws JiraRetrieveDataException, IOException {
        DefaultHttpClient client = new DefaultHttpClient();
        client.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                new UsernamePasswordCredentials(username, password));
        BasicScheme basicAuth = new BasicScheme();
        BasicHttpContext context = new BasicHttpContext();
        context.setAttribute("preemptive-auth", basicAuth);
        client.addRequestInterceptor(new PreemptiveAuth(), 0);

        HttpGet get = new HttpGet(url);
        try 
        {
            HttpResponse response = client.execute(get, context);
            
            int statusCode = response.getStatusLine().getStatusCode();
            if( statusCode != 200 && retries > 0)
            {
                waitBeforeRetry();
                return retrieveData(url, --retries);
            }
            else if (statusCode != 200)
            {
            	throw new JiraRetrieveDataException("Jira returned error status code: " + statusCode);
            }
            
            HttpEntity entity = response.getEntity();
            return EntityUtils.toString(entity);
        }
        catch (IOException e) 
        {
            if (retries > 0)
            {
                waitBeforeRetry();
                return retrieveData(url, --retries);
            }
            throw e;
        }
    }

    private void waitBeforeRetry() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e1) {
            Thread.currentThread().interrupt();
            tracer.exception("waitBeforeRetry", e1);
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

package com.nigealm.agent.impl.jenkins;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import com.google.common.net.UrlEscapers;
import com.nigealm.agent.impl.AgentDataCollectionException;
import com.nigealm.common.utils.Tracer;


public abstract class AbstractJenkinsDataRetriever {

    protected final static Tracer tracer = new Tracer(AbstractJenkinsDataRetriever.class);

    protected String hostname;

    protected String port;

    protected String jobName;

    protected String user;

    protected String password;

    public AbstractJenkinsDataRetriever(String hostname, String port, String jobName){
        this.hostname = hostname;
        this.port = port;
        this.jobName = jobName;
    }

    public AbstractJenkinsDataRetriever(String hostname, String port, String jobName,
                                        String user, String password){
        this(hostname, port, jobName);
        this.user = user;
        this.password = password;
    }

    protected String retrieveJenkinsBuildsWithAuth() throws IOException, AgentDataCollectionException
    {
    	return retrieveJenkinsBuildsWithAuth(3);
    }

    private String retrieveJenkinsBuildsWithAuth(int retries) throws IOException, AgentDataCollectionException 
    {

        try 
        {
            DefaultHttpClient client = new DefaultHttpClient();
            client.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                    new UsernamePasswordCredentials(user, password));
            BasicScheme basicAuth = new BasicScheme();
            BasicHttpContext context = new BasicHttpContext();
            context.setAttribute("preemptive-auth", basicAuth);
            client.addRequestInterceptor(new PreemptiveAuth(), 0);
            String url = buildURL();
            HttpGet get = new HttpGet(url);
            HttpResponse response = client.execute(get, context);
            
            int statusCode = response.getStatusLine().getStatusCode();
            if( statusCode != 200 && retries > 0)
            {
                waitBeforeRetry();
                return retrieveJenkinsBuildsWithAuth(--retries);
            }
            else if (statusCode != 200)
            {
            	throw new AgentDataCollectionException("", "Jenkins", new Exception("Jenkins returned error status " +
                        "code:" +
                        " " +
                        statusCode));
            }

            HttpEntity entity = response.getEntity();
            return EntityUtils.toString(entity);
			
		} 
        catch (IOException e) 
		{
            if (retries > 0)
            {
                waitBeforeRetry();
                return getDataJsonFromJenkinsServer(--retries);
            }
            tracer.exception("getDataJsonFromJenkinsServer",e);
            throw e;
        }
    }

    protected String getDataJsonFromJenkinsServer() throws IOException {
        return getDataJsonFromJenkinsServer(3);
    }

    private String getDataJsonFromJenkinsServer(int retries) throws IOException {
        String fullUrl = buildURL();
        HttpURLConnection connection = null;
        URL url;
        try {
            url = new URL(fullUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/xml");

            if (connection.getResponseCode() != 200) {
                if (retries > 0){
                    tracer.trace("Jenkins server returned error " + connection.getResponseCode() + ", " + connection
                            .getResponseMessage() + ". Retries left");
                    waitBeforeRetry();
                    return getDataJsonFromJenkinsServer(--retries);
                }
                tracer.warn("Failed : HTTP error code : "
                        + connection.getResponseCode());
                return "";
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (connection.getInputStream())));

            String output;
            StringBuilder build = new StringBuilder();
            while ((output = br.readLine()) != null) {
                build.append(output);
            }
            return build.toString();
        } catch (IOException e)
        {
            if (retries > 0){
                waitBeforeRetry();
                return getDataJsonFromJenkinsServer(--retries);
            }
            tracer.exception("getDataJsonFromJenkinsServer",e);
            throw e;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String buildURL() {
        String url;
		if (port == null || port.equalsIgnoreCase(""))
		{
            url = hostname;
		}
		else
		{
            url = hostname + ":" + port;
        }
        url += "/job/" + escape(jobName) + "/api/json?depth=1";
        tracer.trace("Jenkins URL to retrieve data is:  " + url);
        return url;
    }

    public String escape(String url) {
        return UrlEscapers.urlPathSegmentEscaper().escape(url);
    }

    static class PreemptiveAuth implements HttpRequestInterceptor {
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

    private void waitBeforeRetry() {
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e1) {
            Thread.currentThread().interrupt();
            tracer.exception("waitBeforeRetry", e1);
        }
    }



}

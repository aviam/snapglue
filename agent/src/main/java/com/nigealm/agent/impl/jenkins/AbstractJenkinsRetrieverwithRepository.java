package com.nigealm.agent.impl.jenkins;

import com.google.common.net.UrlEscapers;
import com.nigealm.agent.impl.AgentDataCollectionException;
import com.nigealm.common.utils.Tracer;
import org.apache.commons.codec.binary.Base64;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by avia on 5/19/16.
 */
public  abstract class AbstractJenkinsRetrieverwithRepository {
    protected final static Tracer tracer = new Tracer(AbstractJenkinsDataRetriever.class);

    protected String hostname;

    protected String port;

    protected String user;
    protected String gitrepo;
    protected String password;
    private final String jobsUrl="/api/json?pretty=true";
    private final String jobsConfigUrl="";

    public AbstractJenkinsRetrieverwithRepository(String hostname, String port,String gitrepo){
        this.hostname = hostname;
        this.port = port;
        this.gitrepo=gitrepo;

    }

    public AbstractJenkinsRetrieverwithRepository(String hostname, String port,
                                        String user, String password, String gitrepo){
        this(hostname, port,gitrepo);
        this.user = user;
        this.password = password;
    }

    protected List<String> retrieveGitReposWithAuth() throws IOException, AgentDataCollectionException
    {
        return retrieveGitReposWithAuth(3);
    }

    private List<String> retrieveGitReposWithAuth(int retries) throws IOException, AgentDataCollectionException
    {
        List <String> repoNmaes=new ArrayList<>();
        List<String>  jobNames=new ArrayList<>();
        try
        {
            DefaultHttpClient client = new DefaultHttpClient();
            client.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                    new UsernamePasswordCredentials(user, password));
            BasicScheme basicAuth = new BasicScheme();
            BasicHttpContext context = new BasicHttpContext();
            context.setAttribute("preemptive-auth", basicAuth);
            client.addRequestInterceptor(new PreemptiveAuth(), 0);
            String url = buildURL(this.jobsUrl);
            HttpGet get = new HttpGet(url);
            HttpResponse response = client.execute(get, context);

            int statusCode = response.getStatusLine().getStatusCode();
            if( statusCode != 200 && retries > 0)
            {
                waitBeforeRetry();
                return retrieveGitReposWithAuth(--retries);
            }
            else if (statusCode != 200)
            {
                throw new AgentDataCollectionException("", "Jenkins", new Exception("Jenkins returned error status " +
                        "code:" +
                        " " +
                        statusCode));
            }

            HttpEntity entity = response.getEntity();
            String json=EntityUtils.toString(entity);
            JSONObject jsonObJ= null;
            JSONArray jsonMainArr=null;
            try {
                jsonObJ = new JSONObject(json);
                jsonMainArr = new JSONArray(jsonObJ.getJSONArray("jobs"));
                for (int i = 0; i < jsonMainArr.length(); i++) {
                    JSONObject childJSONObject = jsonMainArr.getJSONObject(i);
                    String color = childJSONObject.getString("color");
                    String jobName;
                    jobName = childJSONObject.getString("name");
                    if (!color.equalsIgnoreCase("disabled")){
                        repoNmaes=getRepoFromConfigWithAuth(jobName,3);
                        if (!repoNmaes.isEmpty() && repoNmaes.contains(this.gitrepo)){
                            //retrieveJenkinsBuildsWithAuth(retries,jobName);
                            jobNames.add(jobName);
                        }
                    }

                  return jobNames;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }


        }
        catch (IOException e)
        {
            if (retries > 0)
            {
                waitBeforeRetry();
                return retrieveGitReposWithAuth(--retries);
            }
            tracer.exception("getDataJsonFromJenkinsServer",e);
            throw e;
        }
        return jobNames;
    }


    private String retrieveJenkinsBuildsWithAuth(int retries, String JobName) throws IOException, AgentDataCollectionException
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
            String url = buildURLJob(JobName);
            HttpGet get = new HttpGet(url);
            HttpResponse response = client.execute(get, context);

            int statusCode = response.getStatusLine().getStatusCode();
            if( statusCode != 200 && retries > 0)
            {
                waitBeforeRetry();
                return retrieveJenkinsBuildsWithAuth(--retries,JobName);
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

    private String buildURLJob(String jobName){
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
    protected String getDataJsonFromJenkinsServer() throws IOException {
        return getDataJsonFromJenkinsServer(3);
    }

    private String getDataJsonFromJenkinsServer(int retries) throws IOException {
        String fullUrl = buildURL(this.jobsUrl);
        HttpURLConnection connection = null;
        URL url;
        try {
            url = new URL(fullUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/xml");

            if (connection.getResponseCode() != 200) {
                if (retries > 0){
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
    private  List<String> getRepoFromConfigWithAuth(String jobName,int retries) throws IOException, AgentDataCollectionException {
        String repoName=null;
        List<String> repoNames=new ArrayList<>();
        try

        {

            DefaultHttpClient client = new DefaultHttpClient();
            client.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                    new UsernamePasswordCredentials(user, password));

            String urlStr = buildURL("jenkins/job/" + jobName + "/config.xml");
            URL url = new URL(urlStr);
            HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();

            if (url.getUserInfo() != null) {
                String basicAuth = "Basic " + new String(new Base64().encode(url.getUserInfo().getBytes()));
                urlConnection.setRequestProperty("Authorization", basicAuth);
            }



            if( urlConnection.getResponseCode() != 200 && retries > 0)
            {
                waitBeforeRetry();
                return getRepoFromConfigWithAuth(jobName,--retries);
            }
            else if (urlConnection.getResponseCode()  != 200)
            {
                throw new AgentDataCollectionException("", "Jenkins", new Exception("Jenkins returned error status " +
                        "code:" +
                        " " +
                        urlConnection.getResponseCode() ));
            }
            InputStream xml = urlConnection.getInputStream();


            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(xml);
            doc.getDocumentElement().normalize();

            NodeList nodeList = doc.getElementsByTagName("userRemoteConfigs");
            for (int i = 0; i < nodeList.getLength(); i++)
            {
                Node node = nodeList.item(i);

                Element fstElmnt = (Element) node;

                Element userRemoteConfig = (Element) fstElmnt.getElementsByTagName("hudson.plugins.git.UserRemoteConfig");

                Element urlElement = (Element) userRemoteConfig.getElementsByTagName("url");
                String urlRepo=urlElement.toString();
                String repoNameTemp=urlRepo.substring(urlRepo.indexOf(":")+1,urlRepo.lastIndexOf(".")-1);
                repoNames.add(repoNameTemp);


            }





            return repoNames;

        }
        catch (IOException e)
        {
            if (retries > 0)
            {
                waitBeforeRetry();
                return getRepoFromConfigWithAuth(jobName,--retries);
            }
            tracer.exception("getDataJsonFromJenkinsServer",e);
            throw e;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }


        return repoNames;
    }
    private String buildURL(String extendUrl) {
        String url;
        if (port == null || port.equalsIgnoreCase(""))
        {
            url = hostname;
        }
        else
        {
            url = hostname + ":" + port;
        }
        url += extendUrl;
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
            Thread.sleep(5000);
        } catch (InterruptedException e1) {
            Thread.currentThread().interrupt();
            tracer.exception("waitBeforeRetry", e1);
        }
    }


}

package com.nigealm.agent;

import com.nigealm.agent.listeners.AgentStartupListener;
import com.nigealm.agent.svc.MongoDBAgentServiceImpl;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by c5241527 on 4/5/16.
 */

public class RunMultipleConfigurationsTest1 {
    private final String test1configurationJson1 = "{\"name\":\"Test";
    private final String test1configurationJson2 = ",\"project\":\"snapglue\"," +
            "\"tools\":{\"jenkins\":{\"hostName\":\"snapglue.ddns.net\",\"port\":\"8082\"," +
            "\"projectName\":\"SNAPGLUE\"}," +
            "\"jira\":{\"jiraServerUri\":\"http:\\/\\/snapglue.ddns.net:9090\",\"userName\":\"avia\"," +
            "\"password\":\"1q2w3e4r\",\"projectName\":\"SNAPGLUE\"},\"gitlab\":{\"serverUrl\":\"http:\\/\\/snapglue" +
            ".ddns.net:7070\",\"token\":\"6reXDcEFTD7swoQFGanW\",\"projectName\":\"SNAPGLUE\"," +
            "\"branchName\":\"master\",\"useOAuth2Str\":\"false\"}},\"available\":false}";

    private final String test2configurationJson1 = "{\"name\":\"Test";
    private final String test2configurationJson2 = ",\"project\":\"snapglue\"," +
            "\"tools\":{\"jenkins\":{\"hostName\":\"scm/jenkins\",\"port\":\"\"," +
            "\"projectName\":\"EO_COMPILE_AND_TEST_Linux\"}, \"user\" : \"liory\", \"password\" : \"ggmimi99v!$\"}," +
            "\"jira\":{\"jiraServerUri\":\"http:\\/\\/jira:8080\",\"userName\":\"liory\"," +
            "\"password\":\"ggmimi99v!$\",\"projectName\":\"EO\"},\"gitlab\":{\"serverUrl\":\"http:\\/\\/gitcore" +
            ".earnix.local\",\"token\":\"Z2ZjTXYzbNjMz8JCGfXE\",\"projectName\":\"EO\"," +
            "\"branchName\":\"master\",\"useOAuth2Str\":\"false\"}},\"available\":false}";

    @Before
    public void initAgent() {
        AgentStartupListener agentStartupListener = new AgentStartupListener();
        agentStartupListener.initDBProperties();
    }

    @Test
    public void runMultipleConfigurationsTest1() throws IOException {


        MongoDBAgentServiceImpl mongoDBAgentServiceImpl = new MongoDBAgentServiceImpl();


        for (int i = 1; i <= 50; i++) {
            String confId = mongoDBAgentServiceImpl.addConfigurationForTest(test1configurationJson1 + i + '"' +
                    test1configurationJson2);
            String url = "http://46.101.198.128:8080/agentServer/rest/api/startAgentJob?confId=" + confId;
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            // optional default is GET
            con.setRequestMethod("GET");
            int responseCode = con.getResponseCode();
            System.out.println("\nSending 'GET' request to URL : " + url);
            System.out.println("Response Code : " + responseCode);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            //print result
            System.out.println(response.toString());

        }


    }


    @Test
    public void runMultipleConfigurationsTest2() throws IOException {


        MongoDBAgentServiceImpl mongoDBAgentServiceImpl = new MongoDBAgentServiceImpl();


        for (int i = 1; i <= 3; i++) {
            String confId = mongoDBAgentServiceImpl.addConfigurationForTest(test2configurationJson1 + i + '"' +
                    test2configurationJson2);
            String url = "http://localhost:8080/agentServer/rest/api/startAgentJob?confId=" + confId;
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            // optional default is GET
            con.setRequestMethod("GET");
            int responseCode = con.getResponseCode();
            System.out.println("\nSending 'GET' request to URL : " + url);
            System.out.println("Response Code : " + responseCode);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            //print result
            System.out.println(response.toString());

        }


    }

}

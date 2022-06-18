package com.nigealm.agent;

import com.mongodb.client.result.DeleteResult;
import com.nigealm.agent.listeners.AgentStartupListener;
import com.nigealm.agent.svc.MongoDBAgentServiceImpl;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RunMultipleConfigurationsTest {
    private static final String AGENT_HOST = "localhost";
    private static int NUMBER_OF_CONFIGURATIONS = 100;

    private final String configurationJson1 = "{\"name\":\"Test";
    private final String configurationJson2 = ",\"project\":\"snapglue\"," +
            "\"tools\":{\"jenkins\":{\"hostName\":\"snapglue.ddns.net\",\"port\":\"8082\"," +
            "\"projectName\":\"SNAPGLUE\"}," +
            "\"jira\":{\"jiraServerUri\":\"http:\\/\\/snapglue.ddns.net:9090\",\"userName\":\"avia\"," +
            "\"password\":\"1q2w3e4r\",\"projectName\":\"SNAPGLUE\"},\"gitlab\":{\"serverUrl\":\"http:\\/\\/snapglue" +
            ".ddns.net:7070\",\"token\":\"6reXDcEFTD7swoQFGanW\",\"projectName\":\"SNAPGLUE\"," +
            "\"branchName\":\"master\",\"useOAuth2Str\":\"false\"}},\"available\":false}";

    @Before
    public void initAgent() {
        AgentStartupListener agentStartupListener = new AgentStartupListener();
        agentStartupListener.initDBProperties();
    }

    @Test
    public void runMultipleConfigurations() throws Exception {
        List<String> configurationIds = new LinkedList<>();
        MongoDBAgentServiceImpl mongoDBAgentServiceImpl = new MongoDBAgentServiceImpl();
        TestServer testServer;
        try {
            testServer = new TestServer(mongoDBAgentServiceImpl);
            testServer.startServer(5555);
            addConfigurations(mongoDBAgentServiceImpl, NUMBER_OF_CONFIGURATIONS, configurationIds);
            startAgentForConfigurations(configurationIds);
            testServer.join();
            mongoDBAgentServiceImpl.getMongoClient().close();
            stopAgentForConfigurations(configurationIds);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopAgentForConfigurations(List<String> configurationIds) throws IOException, InterruptedException {
        ThreadPoolExecutor poolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(100);
        for (final String confId : configurationIds) {
            poolExecutor.submit(
                    new Thread() {
                        public void run() {
                            try {
                                String url = "http://" + AGENT_HOST + ":8080/agentServer/rest/api/stopAgentJob?confId=" +

                                        confId;
                                URL obj = new URL(url);
                                System.out.println("Stopping Agent for Configuration: " + confId);
                                HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                                con.setRequestMethod("GET");
                                int responseCode = con.getResponseCode();
                                System.out.println("Response Code : " + responseCode);
                                BufferedReader in = new BufferedReader(
                                        new InputStreamReader(con.getInputStream()));
                                in.close();
                            }catch (IOException e){
                                e.printStackTrace();
                            }

                        }
                    });
        }
        poolExecutor.shutdown();
        poolExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

    private void addConfigurations(MongoDBAgentServiceImpl mongoDBAgentServiceImpl, int numberOfConfigurationsToAdd,
                                   List<String> configurationIds) {
        for (int i = 1; i <= numberOfConfigurationsToAdd; i++) {
            String confId = mongoDBAgentServiceImpl.addConfigurationForTest(configurationJson1 + i + '"' +
                    configurationJson2);
            configurationIds.add(confId);
        }
    }

    private void startAgentForConfigurations(List<String> configurationIds) throws IOException {
        for (String confId : configurationIds) {
            String url = "http://" + AGENT_HOST + ":8080/agentServer/rest/api/startAgentJob?confId=" + confId +
                    "&notify_url=http://localhost:5555";
            URL obj = new URL(url);
            System.out.println("Starting Agent for Configuration: " + confId);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            int responseCode = con.getResponseCode();
            System.out.println("Response Code : " + responseCode);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            in.close();
        }
    }


    private class TestServer extends AbstractHandler {
        Server server;
        AtomicInteger configurationsDeletedCounter;
        private MongoDBAgentServiceImpl mongoDBAgentServiceImpl;

        public TestServer(MongoDBAgentServiceImpl mongoDBAgentServiceImpl) {
            this.mongoDBAgentServiceImpl = mongoDBAgentServiceImpl;
            configurationsDeletedCounter = new AtomicInteger(0);
        }

        public void startServer(int port) throws Exception {
            server = new Server(port);
            server.setHandler(this);
            server.start();
        }

        public void join() throws InterruptedException {
            server.join();
        }

        @Override
        public void handle(String s, HttpServletRequest request, HttpServletResponse response, int i) throws
                IOException, ServletException {
            Request base_request = (request instanceof Request) ? (Request) request : HttpConnection
                    .getCurrentConnection()
                    .getRequest();
            base_request.setHandled(true);
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("Done");

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(request.getInputStream()));
            String configurationIdJson = in.readLine();
            in.close();
            cleanDB(configurationIdJson);
            synchronized (this) {
                if (configurationsDeletedCounter.incrementAndGet() == NUMBER_OF_CONFIGURATIONS) {
                    new Thread() {
                        public void run() {
                            try {
                                System.out.println("Shutting down the server...");
                                server.stop();
                                System.out.println("Server has stopped.");
                            } catch (Exception ex) {
                                System.out.println("Error when stopping Jetty server: " + ex.getMessage());
                            }
                        }
                    }.start();
                }
            }
        }

        private void cleanDB(String configurationIdJson) {
            Document document = Document.parse(configurationIdJson);
            String confId = document.getString("configuration_id").toString();
            mongoDBAgentServiceImpl.deleteConfigurationByID(confId);
            cleanTable(document, "AGENT_EXECUTION_META_DATA");
            cleanTable(document, "DAILYREPORTELEMENTS");
            cleanTable(document, "EXTERNAL_DATA");
            cleanTable(document, "PROJECTS");
            cleanTable(document, "RULES");
            cleanTable(document, "SPRINTS");
            cleanTable(document, "USERS");
            cleanTable(document, "USERS_PLUGINS_INFO");
        }


        private void cleanTable(Document document, String collectionName) {
            long numberOfDeletedDocument;
            DeleteResult deleteResult = mongoDBAgentServiceImpl.getMongoDatabase().getCollection
                    (collectionName).deleteMany(document);
            numberOfDeletedDocument = (deleteResult == null) ? 0 : deleteResult.getDeletedCount();
            String confId = document.getString("configuration_id").toString();
            System.out.println("Configuration: " + confId + " Number Document Deleted from " +
                    "collection " + collectionName + " is " + numberOfDeletedDocument);
        }
    }


}

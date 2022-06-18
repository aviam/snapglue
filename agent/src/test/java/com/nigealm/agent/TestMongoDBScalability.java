package com.nigealm.agent;

import com.mongodb.client.FindIterable;
import com.mongodb.client.result.DeleteResult;
import com.nigealm.agent.listeners.AgentStartupListener;
import com.nigealm.agent.svc.MongoDBAgentServiceImpl;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Gil on 07/04/2016.
 */
public class TestMongoDBScalability {

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
        Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
        mongoLogger.setLevel(Level.WARNING);
    }

    @Test
    public void runMultipleConfigurations() throws Exception {
        Queue<String> configurationIds = new ConcurrentLinkedDeque<>();
        MongoDBAgentServiceImpl mongoDBAgentServiceImpl = new MongoDBAgentServiceImpl();
        addConfigurations(mongoDBAgentServiceImpl, NUMBER_OF_CONFIGURATIONS, configurationIds);
        fetchConfigurations(mongoDBAgentServiceImpl, NUMBER_OF_CONFIGURATIONS, configurationIds);
        cleanDB(mongoDBAgentServiceImpl, configurationIds);
        mongoDBAgentServiceImpl.getMongoClient().close();
    }

    private void fetchConfigurations(final MongoDBAgentServiceImpl mongoDBAgentServiceImpl, int numberOfConfigurations,
                                     final Queue<String> configurationIds) {
        Iterator<String> iterator = configurationIds.iterator();
        while (iterator.hasNext()) {
            final String currConfID = iterator.next();
            new Thread() {
                public void run() {
                    FindIterable<Document> allDocuments = mongoDBAgentServiceImpl.getAllDocuments();
                    boolean found = false;
                    for (org.bson.Document document : allDocuments) {
                        String currentConfId = document.get("_id").toString();
                        if (!currConfID.equals(currentConfId)) {
                            continue;
                        }
                        found = true;
                    }
                    if (!found) {
                        System.out.println("Configuration not found");
                    } else {
                        System.out.println("Configuration found");
                    }
                }
            }.start();

        }
    }


    private void addConfigurations(final MongoDBAgentServiceImpl mongoDBAgentServiceImpl, int
            numberOfConfigurationsToAdd,
                                   final Queue<String> configurationIds) throws InterruptedException {
        ThreadPoolExecutor poolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(100);
        for (int i = 1; i <= numberOfConfigurationsToAdd; i++) {
            final int finalI = i;
            poolExecutor.submit(
                    new Thread() {
                        public void run() {
                            String confId = mongoDBAgentServiceImpl.addConfigurationForTest(configurationJson1 + finalI
                                    + '"' +
                                    configurationJson2);
                            System.out.print("Configuration: " + confId + " was added.");
                            configurationIds.add(confId);

                        }
                    });
        }
        poolExecutor.shutdown();
        poolExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

    private void cleanDB(final MongoDBAgentServiceImpl mongoDBAgentServiceImpl, Queue<String> configurationIds) {
        Iterator<String> iterator = configurationIds.iterator();
        while (iterator.hasNext()) {
            String currConfID = iterator.next();
            String json = "{\"configuration_id\" : \"" + currConfID + "\"}";
            cleanDB(mongoDBAgentServiceImpl, json);
        }
    }


    private void cleanDB(final MongoDBAgentServiceImpl mongoDBAgentServiceImpl, String configurationIdJson) {
        Document document = Document.parse(configurationIdJson);
        String confId = document.getString("configuration_id").toString();
        mongoDBAgentServiceImpl.deleteConfigurationByID(confId);
    }


    private void cleanTable(final MongoDBAgentServiceImpl mongoDBAgentServiceImpl, Document document, String
            collectionName) {
        long numberOfDeletedDocument;
        DeleteResult deleteResult = mongoDBAgentServiceImpl.getMongoDatabase().getCollection
                (collectionName).deleteMany(document);
        numberOfDeletedDocument = (deleteResult == null) ? 0 : deleteResult.getDeletedCount();
        String confId = document.getString("configuration_id").toString();
        System.out.println("Configuration: " + confId + " Number Document Deleted from " +
                "collection " + collectionName + " is " + numberOfDeletedDocument);
    }

}

package com.nigealm.agent.svc;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.stereotype.Service;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.nigealm.common.utils.Tracer;

@Consumes({"application/json"})

@Produces({"application/json"})

@Path("/mongo-api")
@Service
public class MongoDBAgentServiceImpl {
    private final static Tracer tracer = new Tracer(MongoDBAgentServiceImpl.class);

    private final static String DB_COLLECTION_NAME = "AgentConfigurationData";

	public static final String MONGO_DB_KEY_HOST = "mongoDBHost";
	public static final String MONGO_DB_KEY_PORT = "mongoDBPort";
	public static final String MONGO_DB_KEY_DB_NAME = "mongoDBName";
	public static final String MONGO_DB_KEY_URI = "mongoDBURI";
	public static final String MONGO_DB_DEFAULT_HOST = "localhost";
	public static final String MONGO_DB_DEFAULT_PORT = "27017";
	public static final String MONGO_DB_DEFAULT_NAME = "snapglue";
	public static String MONGO_DB_HOST = "46.101.98.255";
	public static int MONGO_DB_PORT = 27017;
	public static String MONGO_DB_NAME;
	public static String MONGO_DB_URI;
	
	public static final String AGENT_EXECUTION_KEY_INTERVAL = "interval";
	public static final String AGENT_EXECUTION_KEY_DEFAULT = "7200";
	public static int AGENT_EXECUTION_INTERVAL;
	
    private static MongoClient mongoClient;


    @GET
    @Path("/configurations")
    public String getAllConfigurations() {
        tracer.entry("getConfigurations");
        FindIterable<Document> documents = getAllDocuments();
        MongoCursor<Document> cursor = documents.iterator();
        String json = "";
        JSONArray myjsonarray = new JSONArray();
        while (cursor.hasNext()) {
            Document doc = cursor.next();
            tracer.trace("document: " + doc);
            json = doc.toJson();
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(json);
            } catch (JSONException e) {
                tracer.exception("getAllConfigurations", e);
            }
            myjsonarray.put(jsonObject);
        }
        tracer.exit("getConfigurations");
        return myjsonarray.toString();
    }

    public FindIterable<Document> getAllDocuments() {
        MongoDatabase db = getMongoDatabase();
        FindIterable<Document> documents = db.getCollection(DB_COLLECTION_NAME).find();
        return documents;
    }

    @GET
    @Path("/configurations/{id}")
    public String getConfigurationByID(@PathParam("id") String id) {

        tracer.entry("getConfigurationByID");
        tracer.entry("the id is: " + id);
        JSONObject jsonObject = null;
        Document doc = getDocumentByID(id);
        if (doc != null) {

            try {
                jsonObject = new JSONObject(doc.toJson());
            } catch (JSONException e) {
                tracer.exception("getConfigurationByID", e);
            }
            tracer.entry(jsonObject.toString());
            return jsonObject.toString();
        }
        tracer.info("configuration not found for id: " + id);
        tracer.exit("getConfigurationByID");
        return null;
    }

    @DELETE
    @Path("/configurations/{id}")
    public void deleteConfigurationByID(@PathParam("id") String id) {
        tracer.entry("deleteConfigurationByID");

        Document doc = getDocumentByID(id);
        if (doc != null) {
            removeDocument(doc);
            tracer.info("configuration with id: " + id + " deleted");
        } else {
            tracer.info("configuration not found for id: " + id);
        }
        tracer.exit("deleteConfigurationByID");
    }

    @PUT
    @Path("/configurations/{id}")
    public void updateConfiguration(JSONObject configuration, @PathParam("id") String id) {
        tracer.entry("updateConfiguration");
        tracer.trace("the configuration is: " + configuration);
        Document updatedDoc = Document.parse(configuration.toString());
        updateConfiguration(updatedDoc, id);
    }

    public void updateConfiguration(Document updatedDoc, String id) {
        Document doc = getDocumentByID(id);
        ObjectId objectId = doc.getObjectId("_id");
        updatedDoc.remove("_id");
        updatedDoc.append("_id", objectId);
        Document newDoc = new Document("$set", updatedDoc);
        getMongoDatabase().getCollection(DB_COLLECTION_NAME).updateOne(doc, newDoc);
        tracer.exit("updateConfiguration");
    }

    @POST
    @Path("/configurations")
    public String addConfiguration(String configuration) {
        try {
            tracer.entry("addConfiguration");
            MongoDatabase db = getMongoDatabase();
            Document newConfDocument = Document.parse(configuration);
            newConfDocument.put("available", false);
            db.getCollection(DB_COLLECTION_NAME).insertOne(newConfDocument);
            tracer.info("new configuration added: " + configuration);
            tracer.exit("addConfiguration");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("configuration_id", newConfDocument.get("_id").toString());
            return jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public String addConfigurationForTest(String configuration) {
            MongoDatabase db = getMongoDatabase();
            Document newConfDocument = Document.parse(configuration);
            newConfDocument.put("available", false);
            db.getCollection(DB_COLLECTION_NAME).insertOne(newConfDocument);
            return newConfDocument.get("_id").toString();
    }

    public Document getDocumentByID(String id) {
        tracer.entry("getDocumentByID");
        MongoDatabase db = getMongoDatabase();
        MongoCollection<Document> collection = db.getCollection(DB_COLLECTION_NAME);
        FindIterable<Document> documents = collection.find();
        MongoCursor<Document> cursor = documents.iterator();
        while (cursor.hasNext()) {
            Document doc = cursor.next();
            ObjectId objectId = doc.getObjectId("_id");
            String idString = objectId.toString();
            if (id != null && idString != null && id.equals(idString)) {
                tracer.trace("document: " + doc);
                return doc;
            }
        }

        tracer.info("configuration not found for id: " + id);
        tracer.exit("getDocumentByID");
        return null;
    }

    private void removeDocument(Document doc) {
        MongoDatabase db = getMongoDatabase();
        MongoCollection<Document> collection = db.getCollection(DB_COLLECTION_NAME);
        collection.deleteOne(doc);
    }

    public MongoDatabase getMongoDatabase() {
		return getMongoClient().getDatabase(MONGO_DB_NAME);
    }

	public MongoClient getMongoClient() 
	{
		if (mongoClient == null) 
		{
			synchronized (MongoDBAgentServiceImpl.class)
			{
				if (mongoClient == null) 
				{
					if (MONGO_DB_URI == null)
					{
						mongoClient = new MongoClient(MONGO_DB_HOST,
								MONGO_DB_PORT);

						// mongoClient = new MongoClient();// Connect to default
						// - localhost, 27017
						// mongoClient = new MongoClient("46.101.98.255",
						// 27017);
						// System.out.println("------------------------------------------------------------------");
					} 
					else 
					{
						MongoClientURI uri = new MongoClientURI(MONGO_DB_URI); // connect
																				// using
																				// URI
						mongoClient = new MongoClient(uri);
					}
				}
				// if (mongoClient == null) {
				// mongoClient = new MongoClient(MONGO_DB_HOST, MONGO_DB_PORT);
				// }
			}
		}
		return mongoClient;
	}

}

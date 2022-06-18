package com.nigealm.mongodb;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.mongodb.client.*;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.nigealm.common.utils.DateUtils;
import com.nigealm.common.utils.Pair;
import com.nigealm.common.utils.Tracer;
import com.nigealm.utils.JSONUtils;

@Consumes({ "application/json" })
@Produces({ "application/json" })
@Path("/mongodb")
@Service
public class MongoConnectionManager
{
	private final static Tracer tracer = new Tracer(MongoConnectionManager.class);

	private static MongoConnectionManager INSTANCE = null;

	public static final String MONGO_DB_KEY_HOST = "mongoDBHost";
	public static final String MONGO_DB_KEY_PORT = "mongoDBPort";
	public static final String MONGO_DB_KEY_DB_NAME = "mongoDBName";
	public static final String MONGO_DB_KEY_URI = "mongoDBURI";
	public static final String MONGO_DB_DEFAULT_HOST = "localhost";
	public static final String MONGO_DB_DEFAULT_PORT = "27017";
	public static final String MONGO_DB_DEFAULT_NAME = "snapglue";
	public static String MONGO_DB_HOST;
	public static int MONGO_DB_PORT;
	public static String MONGO_DB_NAME;
	public static String MONGO_DB_URI;

	public static enum MongoDBCollection
	{
		PROJECTS(),

		USERS_PLUGINS_INFO(),

		USERS(),

		RULES(),

		ALERTS(),

		DAILYREPORTELEMENTS(),

		SPRINTS(),

		EXTERNAL_DATA(),

		ISSUES(),

		BUILDS(),

		COMMITS(),

		AGENT_EXECUTION_META_DATA();

    }
	
	private static MongoClient mongoClient;
//	private String dbName;
//	private String collectionName;

	private MongoConnectionManager()
	{
		//do nothing
	}

	public static MongoConnectionManager getInstance()
	{
		if (INSTANCE == null)
		{
			synchronized (MongoConnectionManager.class)
			{
				if (INSTANCE == null)
				{
					INSTANCE = new MongoConnectionManager();
				}
			}
		}

		return INSTANCE;
	}

//	public MongoConnectionManager(final String dbName, final String collectionName)
//	{
//		this.dbName = dbName;
//		this.collectionName = collectionName;
//	}

//	@GET
//	@Path("/configurations")
	private String getAllDocuments(String collectionName)
	{
//		tracer.entry("getConfigurations");
		FindIterable<Document> documents = getAllDocumentsList(collectionName);
		MongoCursor<Document> cursor = documents.iterator();
		String json = "";
		JSONArray myjsonarray = new JSONArray();
		while (cursor.hasNext())
		{
			Document doc = cursor.next();
//			tracer.trace("document: " + doc);
			json = doc.toJson();
			JSONObject jsonObject = null;
			try
			{
				jsonObject = new JSONObject(json);
			}
			catch (JSONException e)
			{
//				tracer.trace("generation of all configuration failed: " + e.getMessage());
			}
			myjsonarray.put(jsonObject);
		}
//		tracer.exit("getConfigurations");
		return myjsonarray.toString();
	}

//	@GET
//	@Path("/configurations/{id}")
	private String getDocumentByID(String collectionName, String id)
	{

//		tracer.entry("getConfigurationByID");
//		tracer.entry("the id is: " + id);
		JSONObject jsonObject = null;
		Document doc = getDocumentbyID(collectionName, id);
		if (doc != null)
		{

			try
			{
				jsonObject = new JSONObject(doc.toJson());
			}
			catch (JSONException e)
			{

//				tracer.trace("getting configuration " + id + " failed: " + e.getMessage());
			}
//			tracer.entry(jsonObject.toString());
			return jsonObject.toString();
		}
//		tracer.trace("configuration not found for id: " + id);
//		tracer.exit("getConfigurationByID");
		return null;
	}

	private String getDocumentByAnyField(String collectionName, String key, String valueKey)
	{

//		tracer.entry("getConfigurationByField");
//		tracer.entry("the key is: " + key);
		JSONObject jsonObject = null;
		Document doc = getDocumentbyField(collectionName, key, valueKey);
		if (doc != null)
		{

			try
			{
				jsonObject = new JSONObject(doc.toJson());
			}
			catch (JSONException e)
			{

//				tracer.trace("getting configuration " + valueKey + " failed: " + e.getMessage());
			}
//			tracer.entry(jsonObject.toString());
			return jsonObject.toString();
		}
//		tracer.trace("configuration not found for field: " + valueKey);
//		tracer.exit("getConfigurationByField");
		return null;
	}
	public static void deleteDocumentByID(String collectionName, String id)
	{
//		tracer.entry("deleteConfigurationByID");

		Document doc = getDocumentbyID(collectionName, id);
		if (doc != null)
		{
			removeDocument(collectionName, doc);
//			tracer.trace("configuration with id: " + id + " deleted");
		}
		else
		{
//			tracer.trace("configuration not found for id: " + id);
		}
//		tracer.exit("deleteConfigurationByID");
	}

	public static void updateDocument(String collectionName, Document document, String id)
	{
//		tracer.entry("updateDocument");
//		tracer.entry("the id is: " + id);
//		tracer.entry("the document is: " + document);

		Document docFromDB = getDocumentbyID(collectionName, id);
//		Document updatedDoc = Document.parse(document.toString());
		Document updatedDoc = document;

		if (docFromDB == null)
		{
//			tracer.trace("document not found for id: " + id);
			return;
		}

		ObjectId objectId = docFromDB.getObjectId("_id");
		updatedDoc.remove("_id");
		updatedDoc.append("_id", objectId);
		Document newDoc = new Document("$set", updatedDoc);
		getCollection(collectionName).updateOne(docFromDB, newDoc);

//		tracer.exit("updateConfiguration");
	}

	@POST
	@Path("/configurations")
	public static void addDocument(String collectionName, String document)
	{
//		tracer.entry("addDocument");
		addDocument(collectionName, Document.parse(document));
//		tracer.exit("addDocument");
	}

	public static void addDocument(String collectionName, Document document)
	{
//		tracer.entry("addDocument");
		getCollection(collectionName).insertOne(document);
//		tracer.trace("new document added: " + document);
//		tracer.exit("addDocument");
	}

	public static FindIterable<Document> findAllDocsInCollection(String collectionName)
	{
		return getCollection(collectionName).find();
	}

	public static FindIterable<Document> findAllDocsInCollectionByValue(String collectionName, String key, Object value)
	{
		FindIterable<Document> iterable = getCollection(collectionName).find(new Document(key, value));
		return iterable;
	}

	public static Document findDocumentInCollectionById(String collectionName, String id)
	{
		MongoCollection<Document> collection = getCollection(collectionName);
		FindIterable<Document> documents = collection.find();
		MongoCursor<Document> cursor = documents.iterator();
		while (cursor.hasNext())
		{
			Document doc = cursor.next();
			ObjectId objectId = doc.getObjectId("_id");
			String idString = objectId.toString();
			if (id != null && idString != null && id.equals(idString))
			{
				return doc;
			}
		}
		return null;
	}
	public static Document findDocumentInCollectionByKeyAndValue(String collectionName, String key, String value)
	{
		MongoCollection<Document> collection = getCollection(collectionName);
		FindIterable<Document> documents = collection.find();
		MongoCursor<Document> cursor = documents.iterator();
		while (cursor.hasNext())
		{
			Document doc = cursor.next();
		
			String keyValue = doc.getString(key);
			if (key != null && keyValue != null && value.equals(keyValue))
			{
				return doc;
			}
		}

		return null;
	}

	public static Document findDocumentInCollectionByKeysAndValues(String collectionName, List<Pair<String, Object>> keysAndValues)
	{
		MongoCollection<Document> collection = getCollection(collectionName);
		FindIterable<Document> documents = collection.find();
		MongoCursor<Document> cursor = documents.iterator();
		while (cursor.hasNext())
		{
			Document doc = cursor.next();
		
			for (Pair<String, Object> pair : keysAndValues) 
			{
				String key = pair.getValue1();
				Object value = pair.getValue2();
				
				if(value instanceof String)
				{
					String keyValue = doc.getString(key);
					if (key != null && keyValue != null && value.equals(keyValue))
					{
						return doc;
					}
				}
				else if (value instanceof Date)
				{
					Date keyValue = doc.getDate(key);
					if (key != null && keyValue != null && value.equals(keyValue))
					{
						return doc;
					}
				}
			}
		}

		return null;
	}

	public static List<Document> findDocumentsInCollectionByFilter(String collectionName, BasicDBObject filter)
	{
		FindIterable<Document> docs = getCollection(collectionName).find(filter);
		if(docs == null)
			return new ArrayList<>();
		List<Document> res = JSONUtils.convertIterableToList(docs.iterator());
		return res;
	}

    public static List<Document> doAggregationOnCollection(String collectionName, List<Document> aggregateFields)
    {

        AggregateIterable<Document> iterable = getCollection(collectionName).aggregate(aggregateFields);

        if(iterable == null)
            return new ArrayList<>();
        List<Document> res = JSONUtils.convertIterableToList(iterable.iterator());
        return res;
    }
	
	public static List<Document> findDocumentsInCollectionByKeysAndValues(String collectionName, List<Pair<String, Object>> keysAndValues)
	{
		List<Document> res = new ArrayList<>();
		
		MongoCollection<Document> collection = getCollection(collectionName);
		FindIterable<Document> documents = collection.find();
		MongoCursor<Document> cursor = documents.iterator();
		while (cursor.hasNext())
		{
			Document doc = cursor.next();
		
			boolean conditionsFullfilled = true;
			
			for (Pair<String, Object> pair : keysAndValues) 
			{
				String key = pair.getValue1();
				Object value = pair.getValue2();
				
				Object keyValue = null;
				if(value instanceof String)
				{
					keyValue = doc.getString(key);
				}
				else if (value instanceof Date)
				{
					Date date = DateUtils.getDateInUTCMidnight(doc.getDate(key));
					keyValue = date;
				}
				
				if (key != null && keyValue != null && value.equals(keyValue))
				{
					conditionsFullfilled &= true;
				}
				else
				{
					conditionsFullfilled = false;
				}
			}
			
			if(conditionsFullfilled)
				res.add(doc);
		}

		return res;
	}

	public static Document findLastDocInCollection(String collectionName, String sortByKeyName)
	{
		FindIterable<Document> documents = getDocsSorted(collectionName, sortByKeyName);
		MongoCursor<Document> cursor = documents.iterator();
		if (cursor.hasNext())
			return cursor.next();
		else
			return null;
	}

	public static FindIterable<Document> getDocsSorted(String collectionName, String sortByKeyName)
	{
		MongoCollection<Document> collection = getCollection(collectionName);

		BasicDBObject query = new BasicDBObject();
		query.put(sortByKeyName, new BasicDBObject("$lt", sortByKeyName));
		FindIterable<Document> documents = collection.find(query).sort(new BasicDBObject(sortByKeyName, "-1"))
				.limit(10);
		return documents;
	}

	private static Document getDocumentbyID(String collectionName, String id)
	{
//		tracer.entry("getDocumentbyID");

		FindIterable<Document> documents = getCollection(collectionName).find();
		MongoCursor<Document> cursor = documents.iterator();
		while (cursor.hasNext())
		{
			Document doc = cursor.next();
			ObjectId objectId = doc.getObjectId("_id");
			String idString = objectId.toString();
			if (id != null && idString != null && id.equals(idString))
			{
//				tracer.trace("document: " + doc);
				return doc;
			}
		}

//		tracer.trace("configuration not found for id: " + id);
//		tracer.exit("getDocumentbyID");
		return null;
	}
	private static Document getDocumentbyField(String collectionName, String key,String valueName)
	{
//		tracer.entry("getDocumentbyName");

		FindIterable<Document> documents = getCollection(collectionName).find();
		MongoCursor<Document> cursor = documents.iterator();
		while (cursor.hasNext())
		{
			Document doc = cursor.next();
			
			String valueString = doc.getString(key);
			if (valueString != null  && valueString.equals(valueName))
			{
//				tracer.trace("document: " + doc);
				return doc;
			}
		}

//		tracer.trace("configuration not found for id: " + valueName);
//		tracer.exit("getDocumentbyName");
		return null;
	}
	public static void removeDocument(String collectionName, Document doc)
	{
		getCollection(collectionName).deleteOne(doc);
	}

	public static void removeAllDocuments(String collectionName)
	{
		Bson filter = new BasicDBObject();
		getCollection(collectionName).deleteMany(filter);
	}

	public static void removeDocumentsFrom(String collectionName, FindIterable<Document> documents)
	{
		MongoCursor<Document> cursor = documents.iterator();
		while (cursor.hasNext())
		{
			Document doc = cursor.next();
			removeDocument(collectionName, doc);
		}
	}

	private FindIterable<Document> getAllDocumentsList(String collectionName)
	{
		return getCollection(collectionName).find();
	}

	private static MongoCollection<Document> getCollection(String collectionName)
	{
		return getInstance().getCollectionByName(collectionName);
	}

	private MongoCollection<Document> getCollectionByName(String collectionName)
	{
		return getDatabase().getCollection(collectionName);
	}

	private MongoDatabase getDatabase()
	{
		return getMongoClient().getDatabase(MONGO_DB_NAME);
	}

	private MongoClient getMongoClient()
	{
		if (mongoClient == null) 
		{
			synchronized (MongoConnectionManager.class)
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
				tracer.trace(mongoClient.toString());
			}
		}
		return mongoClient;
	}

	private void closeMongoClient()
	{
		if (mongoClient == null)
			return;

		mongoClient.close();
	}

	private Document find(String collectionName)
	{
//		tracer.entry("find");
		Document d = getCollection(collectionName).find().iterator().next();
//		tracer.trace(d.toString());
//		tracer.exit("find");
		return d;
	}

}

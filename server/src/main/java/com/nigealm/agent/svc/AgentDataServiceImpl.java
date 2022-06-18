package com.nigealm.agent.svc;

import java.util.Calendar;
import java.util.Date;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.bson.Document;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import com.mongodb.client.FindIterable;
import com.nigealm.common.utils.Tracer;
import com.nigealm.mongodb.MongoConnectionManager;
import com.nigealm.mongodb.MongoConnectionManager.MongoDBCollection;
import com.nigealm.mongodb.MongoUtils;

@Consumes(
        {"application/json"})
@Produces(
        {"application/json"})
@Path("/agentservice")
@Service
public class AgentDataServiceImpl implements AgentDataService
{
	private final static Tracer tracer = new Tracer(AgentDataServiceImpl.class);

	public static class AgentLastExecutionData
	{
		private String lastExecutionTimeInMillis;
		private int lastBuildNumber;

		public AgentLastExecutionData(String lastExecutionTimeInMillis, int lastBuildNumber)
		{
			this.lastExecutionTimeInMillis = lastExecutionTimeInMillis != null ? lastExecutionTimeInMillis
					: String.valueOf(new Date(0).getTime());
			this.lastBuildNumber = lastBuildNumber;
		}

		/**
		 * @return the lastExecutionTime
		 */
		public String getLastExecutionTime()
		{
			return lastExecutionTimeInMillis;
		}

		/**
		 * @return the lastBuildNumber
		 */
		public int getLastBuildNumber()
		{
			return lastBuildNumber;
		}
	}

	@GET
	@Path("/data")
	@PreAuthorize("hasRole('ROLE_USER')")
	public AgentLastExecutionData getAgentLastExecutionData(String configID)
	{
		tracer.entry("getAgentLastExecutionTime");

		String lastExecutionTimeInMillis = null;
		int lastBuildNumber = 0;
		Document agentData = MongoConnectionManager.findDocumentInCollectionByKeyAndValue(
				MongoDBCollection.AGENT_EXECUTION_META_DATA.name(), MongoUtils.DOCUMENT_KEY_CONFIGֹID, configID);

		if (agentData != null)
		{
			lastExecutionTimeInMillis = MongoUtils.getLastSuccessTimeFromAgentExecutionDataDoc(agentData);
			lastBuildNumber = MongoUtils.getLastBuildNumberFromAgentExecutionDataDoc(agentData);
		}

		String time = "not found";
		if (lastExecutionTimeInMillis != null)
		{
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(Long.parseLong(lastExecutionTimeInMillis));
			time = cal.getTime().toString();
		}

		tracer.trace("last agent execution data:" + time + ", build number:" + lastBuildNumber);
		tracer.exit("getAgentLastExecutionTime");
		return new AgentLastExecutionData(lastExecutionTimeInMillis, lastBuildNumber);
	}

	public void updateAgentExecutionData(int lastBuildNumber, String configID, boolean updateExecutionDate)
	{
		//get docs for configuration id
		FindIterable<Document> lastExecutionDocs = MongoConnectionManager.findAllDocsInCollectionByValue(
				MongoDBCollection.AGENT_EXECUTION_META_DATA.name(), MongoUtils.DOCUMENT_KEY_CONFIGֹID, configID);

		//if exist 
		Date startOfTime = new Date(0);
		long lastExecutionTimeInMillisFromDB = startOfTime.getTime();
		if (lastExecutionDocs != null && lastExecutionDocs.iterator().hasNext())
		{
			//get the data from db...
			Document lastExecutionDoc = lastExecutionDocs.iterator().next();
			int lastBuildNumberFromDB = MongoUtils.getLastBuildNumberFromAgentExecutionDataDoc(lastExecutionDoc);
			lastBuildNumber = lastBuildNumber > lastBuildNumberFromDB ? lastBuildNumber : lastBuildNumberFromDB;
			
			String lastExecutionTimeInMillisStr = MongoUtils.getLastSuccessTimeFromAgentExecutionDataDoc(lastExecutionDoc);
			lastExecutionTimeInMillisFromDB = Long.parseLong(lastExecutionTimeInMillisStr);
		}

		//remove all docs
		MongoConnectionManager.removeDocumentsFrom(MongoDBCollection.AGENT_EXECUTION_META_DATA.name(),
				lastExecutionDocs);

		//now update
		// build number only if > 0
		// execution date according to boolean parameter provided
		Date currentTime = new Date();

		long millisToSave = updateExecutionDate ? currentTime.getTime() : lastExecutionTimeInMillisFromDB;
		
		//create a new doc
		Document doc = new Document();

		doc.append(MongoUtils.AGENT_EXECUTION_DATA_DOC_KEY_LAST_SUCCESS_TIME, millisToSave);
		doc.append(MongoUtils.AGENT_EXECUTION_DATA_DOC_KEY_LAST_BUILD_NUMBER, lastBuildNumber);
		doc.append(MongoUtils.DOCUMENT_KEY_CONFIGֹID, configID);
		MongoConnectionManager.addDocument(MongoDBCollection.AGENT_EXECUTION_META_DATA.name(), doc);

		tracer.trace(
				"-------------------------------------------------------------------------------------------------------");
		String extraStr = updateExecutionDate ? "Probably last iteration." : "";
		tracer.trace("Execution data saved. " + extraStr + " configuration_id/date/lastBuildNumber: " + configID + "/" + currentTime
				+ "/" + lastBuildNumber);
		tracer.trace(
				"-------------------------------------------------------------------------------------------------------");
	}

}

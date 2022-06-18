package com.nigealm.alerts.svc;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.bson.Document;
import org.json.JSONArray;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.nigealm.mongodb.MongoConnectionManager;
import com.nigealm.mongodb.MongoConnectionManager.MongoDBCollection;
import com.nigealm.mongodb.MongoUtils;
import com.nigealm.utils.JSONUtils;

@Consumes({ "application/json" })
@Produces({ "application/json" })
@Path("/alertsmanagement")
@Service
public class AlertsServiceImpl implements AlertsService
{

	public static enum AlertStatus
	{
		NOT_ACTIVE, ACTIVE
	}

	@Override
	@GET
	@Path("/alerts")
	@PreAuthorize("hasRole('ROLE_USER')")
	public String getActiveAlerts()
	{
		FindIterable<Document> docs = MongoConnectionManager.findAllDocsInCollectionByValue(
				MongoDBCollection.ALERTS.name(), "status", AlertStatus.ACTIVE.name());
		JSONArray res = JSONUtils.convertDocumentListToJSONArray(docs);
		String str = res.toString();
		System.out.println(str);
		return str;

//		List<AlertVO> alertsVO = alertsRepository.findByStatus(AlertStatus.ACTIVE);
//		Collection<AlertDTO> alertsDTO = convertAlertDocToDto(alertsVO);
//		return new ArrayList<AlertDTO>(alertsDTO);
	}

	@Override
	@GET
	@Path("/count")
	@PreAuthorize("hasRole('ROLE_USER')")
	public int getActiveAlertsCount()
	{
		FindIterable<Document> docs = MongoConnectionManager.findAllDocsInCollectionByValue(
				MongoDBCollection.ALERTS.name(), "status", AlertStatus.ACTIVE.name());

		MongoCursor<Document> cursor = docs.iterator();
		int count = 0;
		while (cursor.hasNext())
			count++;

		return count;

	}

	@Override
	public void addOrUpdateAlert(String projectName, String sprintName, String description, long currentTimeMillis,
			String status, String ruleID)
	{
		FindIterable<Document> docs = MongoConnectionManager.findAllDocsInCollectionByValue(
				MongoDBCollection.ALERTS.name(), "status", AlertStatus.ACTIVE.name());
		
		MongoCursor<Document> cursor = docs.iterator();
		Document docFromDB = null;
		while (cursor.hasNext())
		{
			Document currentDoc = cursor.next();
			
			String projectNameVal = currentDoc.getString("projectName");
			String statusVal = currentDoc.getString("status");
			String ruleIDVal = currentDoc.getString("ruleID");
			String sprintNameVal = currentDoc.getString("sprintName");
			
			if (projectNameVal.equals(projectName) && statusVal.equals(statusVal) && ruleIDVal.equals(ruleID)
					&& sprintNameVal != null && sprintNameVal.equals(sprintName))
			{
				docFromDB = currentDoc;
				break;
			}
		}
		
		if (docFromDB != null)
		{
			//exists in db, update doc
			docFromDB.put("message", description);
			String id = MongoUtils.getIDFromDoc(docFromDB);

			//update in db
			MongoConnectionManager.updateDocument(MongoDBCollection.ALERTS.name(), docFromDB, id);
		}
		else
		{
			addAlert(projectName, sprintName, description, currentTimeMillis, status, ruleID);
		}
	}

	@Override
	public void addAlert(String projectName, String sprintName, String description, long currentTimeMillis,
			String status, String ruleID)
	{
		Document newAlertDoc = new Document();
		newAlertDoc.append("projectName", projectName);
		newAlertDoc.append("sprintName", sprintName);
		newAlertDoc.append("ruleID", ruleID);
		newAlertDoc.append("status", AlertStatus.ACTIVE.name());
		newAlertDoc.append("message", description);

		//create
		MongoConnectionManager.addDocument(MongoDBCollection.ALERTS.name(), newAlertDoc);
	}

	@Override
	public void disableAlerts(String projectName, String sprintName, String ruleID)
	{
		FindIterable<Document> alertDocs = MongoConnectionManager
				.findAllDocsInCollectionByValue(MongoDBCollection.ALERTS.name(), "ruleID", ruleID);
		for (Document alertDoc : alertDocs)
		{
			String projectNameVal = MongoUtils.getProjectNameFromAlertDoc(alertDoc);
			String sprintNameVal = MongoUtils.getSprintNameFromAlertDoc(alertDoc);
			if (projectNameVal.equals(projectName) && sprintNameVal.equals(sprintName))
			{
				//disable
				alertDoc.put("status", AlertStatus.NOT_ACTIVE.name());
				String id = MongoUtils.getIDFromDoc(alertDoc);
				MongoConnectionManager.updateDocument(MongoDBCollection.ALERTS.name(), alertDoc, id);
			}
		}
	}

//	@Override
//	@GET
//	@Path("/getSprintActiveAlerts")
//	@PreAuthorize("hasRole('ROLE_USER')")
//	public List<AlertDTO> getSprintActiveAlerts(@QueryParam("project") String project,
//			@QueryParam("version") String version, @QueryParam("sprintName") String sprintName)
//	{
//
//		List<AlertVO> alertsVO;
//		if (StringUtils.isEmpty(sprintName))
//		{
//			alertsVO = alertsRepository.findByProjectAndVersionAndStatus(project, version, AlertStatus.ACTIVE.name());
//		}
//		else
//		{
//			alertsVO = alertsRepository.findByProjectAndVersionAndSprintNameAndStatus(project, version, sprintName,
//					AlertStatus.ACTIVE);
//		}
//		Collection<AlertDTO> alertsDTO = convertAlertDocToDto(alertsVO);
//		return new ArrayList<AlertDTO>(alertsDTO);
//	}

//	@Override
//	@GET
//	@Path("/getRuleActiveAlerts")
//	@PreAuthorize("hasRole('ROLE_USER')")
//	public String getAlertsOfRule(@QueryParam("project") String project,
//			@QueryParam("version") String version, @QueryParam("triggerRuleId") Long triggerRuleId)
//	{
//		FindIterable<Document> docs = MongoConnectionManager.findAllDocsInCollectionByValue(
//				MongoDBCollection.ALERTS.getName(), "triggerRuleId", String.valueOf(triggerRuleId));
//		JSONArray res = MongoConnectionManager.convertDocumentListToJSONArray(docs);
//		return res.toString();
//
//		
//		List<AlertVO> alertsVO = alertsRepository.findByProjectAndVersionAndTriggerRuleIdAndStatus(project, version,
//				triggerRuleId, AlertStatus.ACTIVE);
//		Collection<AlertDTO> alertsDTO = convertAlertDocToDto(alertsVO);
//
//		return new ArrayList<AlertDTO>(alertsDTO);
//	}

//	@Override
//	@GET
//	@Path("/alerts")
//	@PreAuthorize("hasRole('ROLE_USER')")
//	public List<AlertDTO> getAllAlerts()
//	{
//		List<AlertVO> alertsVO = (List<AlertVO>) alertsRepository.findAll();
//		Collection<AlertDTO> alertsDTO = convertAlertDocToDto(alertsVO);
//		return new ArrayList<AlertDTO>(alertsDTO);
//	}

//	@Override
//	@GET
//	@Path("/alerts/{id}")
//	@PreAuthorize("hasRole('ROLE_USER')")
//	public AlertDTO getAlertById(@PathParam("id") Long id)
//	{
//		AlertVO alertVO = alertsRepository.findOne(id);
//		Collection<AlertDTO> alertsDTO = convertAlertDocToDto(Collections.singletonList(alertVO));
//		return alertsDTO.iterator().next();
//	}

//	@Override
//	@Transactional
//	@Path("/alerts/{id}")
//	@DELETE
//	@PreAuthorize("hasRole('ROLE_USER')")
//	public void deleteAlert(@PathParam("id") Long alertId)
//	{
//		alertsRepository.delete(alertId);
//	}

//	@Override
//	@Transactional
//	public void updateAlerts(List<AlertDTO> alerts)
//	{
//		for (AlertDTO alertDTO : alerts)
//		{
//			AlertVO alert = alertsRepository.findByProjectAndVersionAndId(alertDTO.getProject(), alertDTO.getVersion(),
//					alertDTO.getId());
//			alert.setAlertStatus(alertDTO.getAlertStatus());
//			alertsRepository.save(alert);
//		}
//	}

//	@PUT
//	@Path("/alerts/{id}")
//	@PreAuthorize("hasRole('ROLE_USER')")
//	public void updateAlertById(JSONObject alertObject, @PathParam("id") Long alertId)
//	{
//		AlertStatus status = null;
//
//		try
//		{
//			status = (AlertStatus) alertObject.get("status");
//		}
//		catch (JSONException e)
//		{
//			e.printStackTrace();
//			return;
//		}
//
//		// find the alert by id
//		AlertVO alertVO = alertsRepository.findOne(alertId);
//
//		// update the alert
//		alertVO.setAlertStatus(status);
//
//		// save
//		alertsRepository.save(alertVO);
//	}

//	@Override
//	@Transactional
//	public void addAlert(AlertVO alertVO)
//	{
//		List<AlertVO> alertVOList = alertsRepository.findByProjectAndVersionAndSprintNameAndTriggerRuleIdAndStatus(
//				alertVO.getProject(), alertVO.getVersion(), alertVO.getSprintName(), alertVO.getTriggerRuleId(),
//				alertVO.getAlertStatus());
//		if (alertVOList.isEmpty())
//		{
//			alertsRepository.save(alertVO);
//		}
//	}

//	@Override
//	@Transactional
//	@POST
//	@Path("/alerts")
//	@PreAuthorize("hasRole('ROLE_USER')")
//	public void createAlert(JSONObject alertObject)
//	{
//		String project = null;
//		String version = null;
//		String sprintName = null;
//		AlertStatus status = null;
//		String message = null;
//		long timestamp;
//		long triggerRuleId;
//
//		try
//		{
//			project = (String) alertObject.get("project");
//			version = (String) alertObject.get("version");
//			sprintName = (String) alertObject.get("sprintName");
//			status = (AlertStatus) alertObject.get("status");
//			message = (String) alertObject.get("message");
//			timestamp = ((Long) alertObject.get("params")).longValue();
//			triggerRuleId = ((Long) alertObject.get("triggerRuleId")).longValue();
//		}
//		catch (JSONException e)
//		{
//			e.printStackTrace();
//			return;
//		}
//
//		List<AlertVO> alertVOList = alertsRepository.findByProjectAndVersionAndSprintNameAndTriggerRuleIdAndStatus(
//				project, version, sprintName, triggerRuleId, status);
//		if (alertVOList.isEmpty())
//		{
//			AlertVO alertVO = new AlertVO(project, version, sprintName, message, timestamp, status, triggerRuleId);
//			alertsRepository.save(alertVO);
//		}
//	}

//	private Collection<AlertDTO> convertAlertDocToDto(final List<AlertVO> alertsVO)
//	{
//		Collection<AlertDTO> alertsDTO = CollectionUtils.collect(alertsVO, new Transformer<AlertVO, AlertDTO>()
//		{
//			public AlertDTO transform(AlertVO alertVO)
//			{
//				return new AlertDTO(alertVO.getPrimaryKey(), alertVO.getProject(), alertVO.getVersion(),
//						alertVO.getSprintName(), alertVO.getMessage(), alertVO.getTimestamp(), alertVO.getAlertStatus(),
//						alertVO.getTriggerRuleId());
//			}
//		});
//		return alertsDTO;
//	}

}

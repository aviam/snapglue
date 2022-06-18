package com.nigealm.mongodb;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.nigealm.common.utils.DateUtils;
import com.nigealm.common.utils.Pair;
import com.nigealm.mongodb.MongoConnectionManager.MongoDBCollection;
import com.nigealm.rules.svc.RulesServiceImpl.RuleType;

public final class MongoUtils
{

	private static class DocumentComparator implements Comparator<Document>
	{
		private String key;
		private Class keyType;
		private boolean ascending;
		
		public DocumentComparator(String key, Class keyType, boolean ascending)
		{
			this.key = key;
			this.keyType = keyType;
			this.ascending = ascending;
		}
		
		/**
		 * Note: this comparator imposes orderings that are inconsistent with equals.
		 * This method returns 0 if both objects have identical startDate objects.
		 * The condition (compare(x, y)==0) == (x.equals(y)) will NOT ALWAYS BE TRUE.
		 * Thus, Do NOT use this method when this feature is required (in Set/Map for example).
		 * 
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(Document doc1, Document doc2)
		{
			if (keyType.isAssignableFrom(Long.class))
			{
				Long timestamp1 = doc1.getLong(key);
				Long timestamp2 = doc2.getLong(key);
				
				int res;
				if(ascending)
					res = (timestamp1.compareTo(timestamp2));
				else
					res = (timestamp2.compareTo(timestamp1));
				
				return res;
			}
			else if (keyType.isAssignableFrom(Integer.class))
			{
				Integer timestamp1 = doc1.getInteger(key);
				Integer timestamp2 = doc2.getInteger(key);
				int res;
				if(ascending)
					res = (timestamp1.compareTo(timestamp2));
				else
					res = (timestamp2.compareTo(timestamp1));
				
				return res;
			}
			else if (keyType.isAssignableFrom(String.class))
			{
				String timestamp1 = doc1.getString(key);
				String timestamp2 = doc2.getString(key);
				int res;
				if(ascending)
					res = (timestamp1.compareTo(timestamp2));
				else
					res = (timestamp2.compareTo(timestamp1));
				
				return res;
			}
			else
			{
				return 0;
			}
		}
	}

	public final static String DOCUMENT_KEY_ID = "_id";
	public final static String DOCUMENT_KEY_CONFIGֹID = "configuration_id";
	public final static String DOCUMENT_KEY_INSERTION_DATE = "insertion_date";
	public final static String DOCUMENT_KEY_SPRINT_NAME = "sprintName";
	public final static String DOCUMENT_KEY_PROJECT_NAME = "projectName";
	public final static String DOCUMENT_KEY_SNAPGLUE_USER = "snapglueUser";

	private final static String USERS_PLUGINS_INFO_KEY_FULL_NAME = "fullName";

	public final static String PROJCET_DOC_KEY_PROJCET_NAME = "projectName";
	public final static String PROJCET_DOC_KEY_SPRINT_NAMES = "sprintNames";

	private final static String SPRINT_DOC_KEY_SPRINT_NAME = "name";
	private final static String SPRINT_DOC_KEY_PROJECT_NAME = "projectName";
	private final static String SPRINT_DOC_KEY_SPRINT_CONTENT = "sprintContent";
	private final static String SPRINT_DOC_KEY_SPRINT_INFO = "sprintInfo";
	private final static String SPRINT_DOC_KEY_JIRA = "jira";
	public final static String SPRINT_DOC_KEY_STATE = "state";
	public final static String SPRINT_DOC_KEY_TOTAL_ISSUES_COUNT = "total_issues_count";
	public final static String SPRINT_DOC_KEY_CLOSED_ISSUES_COUNT = "closed_issues_count";
	public final static String SPRINT_DOC_KEY_SPRINT_START_DATE = "start_date";
	public final static String SPRINT_DOC_KEY_SPRINT_END_DATE = "end_date";
	public final static String SPRINT_DOC_KEY_DAILY_DATA = "daily_data";

	public final static String DAILY_DATA_DOC_KEY_DATE = "date";

	public final static String SPRINT_INFO_DOC_KEY_END_DATE = "endDate";
	public final static String SPRINT_INFO_DOC_KEY_START_DATE = "startDate";

	public final static String EXTERNAL_DATA_DOC_KEY_INSERT_TIME = "insertTime";

	private final static String AGENT_DATA_DOC_KEY_EXTERNAL_DATA = "externalData";
	private final static String AGENT_DATA_DOC_KEY_SPRINTS = "sprints";
	private final static String AGENT_DATA_DOC_KEY_USERS = "users";
	private final static String AGENT_DATA_DOC_KEY_SPRINTS_META_DATA = "sprintsMetaData";
	private final static String AGENT_DATA_DOC_KEY_METHOD = "method";
	private final static String AGENT_DATA_DOC_KEY_EXECTUION_TIME = "executionTime";

	private final static String SPRINT_META_DATA_DOC_KEY_STATE = "state";
	private final static String SPRINT_META_DATA_DOC_KEY_NAME = "name";

	private final static String CONTENTS_DOC_KEY_JIRA = "jira";
	private final static String CONTENTS_DOC_KEY_JENKINS = "jenkins";
	private final static String CONTENTS_DOC_KEY_BAMBOO = "bamboo";
	private final static String CONTENTS_DOC_KEY_GITLAB = "gitlab";
	private final static String CONTENTS_DOC_KEY_GITHAB = "github";
	private final static String CONTENTS_DOC_KEY_BITBUCKET = "bitbucket";

	private final static String JIRA_DOC_KEY_ISSUES_INCOMPLETED_COUNT = "incompletedIssuesCount";
	private final static String JIRA_DOC_KEY_ISSUES_COMPLETED_COUNT = "completedIssuesCount";
	private final static String JIRA_DOC_KEY_ISSUES_COMPLETED = "completedIssues";
	private final static String JIRA_DOC_KEY_ISSUES_INCOMPLETED = "incompletedIssues";
	private final static String JIRA_DOC_KEY_ISSUES_PUNTED = "puntedIssues";
	public final static String BUILD_DOC_KEY_BUILD_NUMBER = "number";

	private final static String COMMIT_DOC_KEY_EXECUTION_TIME = "created_at";
	private final static String COMMIT_DOC_KEY_EXECUTOR = "author_name";
	public final static String COMMIT_DOC_KEY_ISSUE_KEY = "related_issues";
	public final static String COMMIT_DOC_KEY_BUILD_NUMBER = "related_build";

	private final static String GITHUB_DOC_KEY_COMMIT = "commit";

	private final static String BITBUCKET_DOC_KEY_DATE = "date";
	private final static String BITBUCKET_DOC_KEY_DESCRIPTION = "message";
	private final static String BITBUCKET_DOC_KEY_AUTHOR = "author";
	private final static String BITBUCKET_AUTHOER_DOC_KEY_USER = "user";
	private final static String BITBUCKET_USER_DOC_KEY_DISPLAY_NAME = "display_name";
	
	private final static String GITHUB_COMMIT_DOC_KEY_COMMITTER = "committer";
	private final static String GITHUB_COMMIT_DOC_KEY_MESSAGE = "message";

	private final static String GITHUB_COMMITTER_DOC_KEY_NAME = "name";
	private final static String GITHUB_COMMITTER_DOC_KEY_DATE = "date";

	private final static String BUILD_DOC_KEY_EXECUTION_TIME = "timestamp";
	private final static String BUILD_DOC_KEY_RESULT = "result";
	private final static String BUILD_DOC_VALUE_RESULT_FAILURE = "FAILURE";
	private final static String BUILD_DOC_VALUE_RESULT_NUMBER = "number";
	private final static String BUILD_DOC_KEY_EXECUTOR = "executor";

	public final static String SPRINT_HISTORY_DOC_KEY_SPRINT_NAME = "sprintName";
	public final static String SPRINT_HISTORY_DOC_KEY_ADDED_AT = "addedAt";
	public final static String SPRINT_HISTORY_DOC_KEY_ADD_DATE = "addDate";

    public final static String ASSIGNEE_HISTORY_DOC_KEY_ASSIGNEE = "assignee";
    public final static String ASSIGNEE_HISTORY_DOC_KEY_ASSIGNED_AT = "assignedAt";
    public final static String ASSIGNEE_HISTORY_DOC_KEY_ASSIGN_DATE = "assignDate";

	public final static String STATUS_HISTORY_DOC_KEY_STATUS = "status";
    public final static String STATUS_HISTORY_DOC_KEY_CHANGED_AT = "changedAt";
    public final static String STATUS_HISTORY_DOC_KEY_CHANGED_DATE = "changeDate";

	public final static String ISSUE_DOC_KEY_URL = "url";
	public final static String ISSUE_DOC_KEY_SPRINTS_DATA = "sprints";
	public final static String ISSUE_DOC_KEY_ASSIGNEES_DATA = "assignees";
	public final static String ISSUE_DOC_KEY_STATUSES_DATA = "statuses";

    private final static String ISSUE_DOC_KEY_FIELDS = "fields";
	public final static String ISSUE_DOC_KEY_CHANGE_LOG = "changelog";
    public final static String ISSUE_DOC_KEY_ASSIGNEE = "assignee";
    public final static String ISSUE_DOC_KEY_STATUS = "status";
	public final static String ISSUE_DOC_KEY_TITLE = "key";
	public final static String ISSUE_DOC_KEY_KEY = "key";
    public final static String ISSUE_DOC_KEY_EXECUTION_DATE = "executionDate";
    public final static String ISSUE_DOC_KEY_LAST_UPDATE = "lastUpdate";
	public final static String ISSUE_CHANGE_LOG_DOC_KEY_HISTORIES = "histories";

	private final static String FIELDS_DOC_KEY_STATUS = "status";
	private final static String FIELDS_DOC_KEY_ASSIGNEE = "assignee";
	private final static String FIELDS_DOC_KEY_CREATED = "created";

	private final static String ASSIGNEE_DOC_KEY_NAME = "displayName";

	private final static String STATUS_DOC_KEY_STATUS = "name";

	public final static String AGENT_EXECUTION_DATA_DOC_KEY_LAST_BUILD_NUMBER = "lastBuildNumber";
	public final static String AGENT_EXECUTION_DATA_DOC_KEY_LAST_SUCCESS_TIME = "lastSuccessTime";

	//history
	public final static String HISTORY_DATA_DOC_KEY_AUTHOR = "author";
	public final static String HISTORY_DATA_DOC_KEY_DATE = "created";
	public final static String HISTORY_DATA_DOC_KEY_DATE_APPENDED_FOR_SORTING = "createdForSorting";
	public final static String HISTORY_DATA_DOC_KEY_ITEMS = "items";
	public final static String HISTORY_DATA_AUTHOR_DOC_KEY_DISPLAY_NAME = "displayName";
	public final static String HISTORY_DATA_ITEMS_DOC_KEY_FIELD = "field";
	public final static String HISTORY_DATA_ITEMS_DOC_KEY_FROM_STRING = "fromString";
	public final static String HISTORY_DATA_ITEMS_DOC_KEY_TO_STRING = "toString";
	

	public final static String RULE_DOC_KEY_KPIS = "kpis";
	public final static String RULE_DOC_BUILD_FAILURE_KEY_HOURS = "hours";
	public static final String RULE_DOC_KEY_TYPE = "type";
	public static final String RULE_DOC_KEY_UPDATE_POLICY = "updatePolicy";
	public static final String RULE_DOC_KEY_ENABLED = "enabled";
	public static final String RULE_DOC_KEY_MESSAGE = "message";
	public final static int RULE_DOC_BUILD_FAILURE_DEFAULT_VALUE_HOURS = 3;
	public static final String RULE_DOC_ISSUE_STATUS_KEY_ISSUE_ID = "issueID";
	public static final String RULE_DOC_ISSUE_STATUS_KEY_DAYS = "days";
	public static final String RULE_DOC_ISSUE_STATUS_KEY_DESIRED_STATUS = "desiredStatus";
	public final static String RULE_DOC_ISSUE_STATUS_DEFAULT_VALUE_ISSUE_ID = "none";
	public final static int RULE_DOC_ISSUE_STATUS_DEFAULT_VALUE_DAYS = 3;
	public final static String RULE_DOC_ISSUE_STATUS_DEFAULT_VALUE_DESIRED_STATUS = "CLOSED";
	public static final String RULE_DOC_SPRINT_STATUS_KEY_REMAINING_DAYS = "remainingDays";
	public static final String RULE_DOC_SPRINT_STATUS_KEY_NUMBER_OF_OPEN_ISSUES = "numberOfOpenIssues";
	public final static int RULE_DOC_SPRINT_STATUS_DEFAULT_VALUE_REMAINING_DAYS = 7;
	public final static int RULE_DOC_SPRINT_STATUS_DEFAULT_VALUE_NUMBER_OF_OPEN_ISSUES = 5;

	public final static String ALERT_DOC_KEY_PROJECT_NAME = "projectName";
	public final static String ALERT_DOC_KEY_SPRINT_NAME = "sprintName";

	public static void saveOrUpdateUser(Document userDoc, String configID)
	{
		String fullName = userDoc.getString(USERS_PLUGINS_INFO_KEY_FULL_NAME);
		Document userInDB = MongoConnectionManager.findDocumentInCollectionByKeyAndValue(
				MongoDBCollection.USERS_PLUGINS_INFO.name(), USERS_PLUGINS_INFO_KEY_FULL_NAME, fullName);

		boolean sameConfigID = false;
		if (userInDB != null)
		{
			String userConfigID = MongoUtils.getConfigIDFromDoc(userInDB);
			sameConfigID = (userConfigID == null && configID == null)
					|| (userConfigID != null && userConfigID.equals(configID));
		}

		if (userInDB != null && sameConfigID)
		{
			//user exists - update
			String id = getIDFromDoc(userInDB);
			MongoConnectionManager.updateDocument(MongoDBCollection.USERS_PLUGINS_INFO.name(), userDoc, id);
		}
		else
		{
			// user does not exist - add new
			userDoc.append(MongoUtils.DOCUMENT_KEY_CONFIGֹID, configID);
			MongoConnectionManager.addDocument(MongoDBCollection.USERS_PLUGINS_INFO.name(), userDoc);
		}
	}

//	public static Document findLastAgentDataDoc()
//	{
//		FindIterable<Document> documents = getDocsSorted(collectionName, sortByKeyName);
//		MongoCursor<Document> cursor = documents.iterator();
//		if (cursor.hasNext())
//			return cursor.next();
//		else
//			return null;
//	}

	public static String getUserNameFromNameInTool(Document userDocument, String nameInTool, String toolName)
	{
		Set<Entry<String, Object>> entries = userDocument.entrySet();
		for (Entry<String, Object> entry : entries)
		{
			String x = entry.getKey();
			Object y = entry.getValue();
		}
		return null;
	}

	public static List<String> getActiveSprintNames()
	{
		return MongoUtils.getActiveSprintNames("EO");
	}

	public static List<String> getActiveSprintNames(String projectName)
	{
		FindIterable<Document> projectDocs = MongoConnectionManager
				.findAllDocsInCollectionByValue(MongoDBCollection.PROJECTS.name(), "projectName", projectName);

		Document projectDoc = projectDocs.iterator().next();
		if (projectDoc == null)
			return new ArrayList<>();

		List<String> sprintNames = getSprintNamesFromProjectDoc(projectDoc);
		return sprintNames;
	}

	public static String getProjectNameFromPrjectDoc(Document projectDoc)
	{
		return projectDoc.getString(PROJCET_DOC_KEY_PROJCET_NAME);

	}

	public static List<String> getSprintNamesFromProjectDoc(Document projectDoc)
	{
		List<String> res = (List<String>) projectDoc.get(PROJCET_DOC_KEY_SPRINT_NAMES);
		if (res == null)
			res = new ArrayList<>();

		return res;
	}

	public static String getSprintNameFromSprintDoc(Document sprintDoc)
	{
		return sprintDoc.getString(SPRINT_DOC_KEY_SPRINT_NAME);

	}

	public static String getMethodFromAgentDoc(Document agentDoc)
	{
		String res = agentDoc.getString(AGENT_DATA_DOC_KEY_METHOD);
		if (res == null)
			res = "none";
		return res;
	}

	public static boolean isLastIteration(Document agentDoc)
	{
		return getExecutionTimeFromAgentDoc(agentDoc) != null;
	}
	
	public static String getExecutionTimeFromAgentDoc(Document agentDoc)
	{
		return agentDoc.getString(AGENT_DATA_DOC_KEY_EXECTUION_TIME);
	}

	public static String getConfigIDFromDoc(Document userDoc)
	{
		String res = userDoc.getString(DOCUMENT_KEY_CONFIGֹID);

		if (res == null)
			res = "none";
		return res;

	}

	public static Date getInsertionDateFromDoc(Document doc)
	{
		return doc.getDate(DOCUMENT_KEY_INSERTION_DATE);
	}

	public static String getProjectNameFromSprintDoc(Document sprintDoc)
	{
		return sprintDoc.getString(SPRINT_DOC_KEY_PROJECT_NAME);
	}

	public static String getProjectNameFromAlertDoc(Document alertDoc)
	{
		return alertDoc.getString(ALERT_DOC_KEY_PROJECT_NAME);
	}

	public static String getSprintNameFromAlertDoc(Document alertDoc)
	{
		return alertDoc.getString(ALERT_DOC_KEY_SPRINT_NAME);
	}

	public static Document getSprintContentFromSprintDoc(Document sprintDoc)
	{
		return (Document) sprintDoc.get(SPRINT_DOC_KEY_SPRINT_CONTENT);
	}

	public static Date getInsertionTimeFromExternalDoc(Document externalDoc)
	{
		long time = externalDoc.getLong(EXTERNAL_DATA_DOC_KEY_INSERT_TIME).longValue();
		return new Date(time);
	}

	public static Document getJiraContent(Document sprintDoc)
	{
		return (Document) sprintDoc.get(SPRINT_DOC_KEY_JIRA);
	}

	public static Document getExternalDataIfExists(Document agentDataDoc)
	{
		return (Document) agentDataDoc.get(AGENT_DATA_DOC_KEY_EXTERNAL_DATA);
	}

	public static Document getSprintInfoDocFromSprintDoc(Document sprintDoc)
	{
		return (Document) sprintDoc.get(SPRINT_DOC_KEY_SPRINT_INFO);
	}

	public static int getSprintRemainingDays(String sprintName)
	{
		Date sprintEndDate = getSprintEndDateFromSprintName(sprintName);
		long currentTimeMillis = System.currentTimeMillis();
		int remainingDays = (int) TimeUnit.MILLISECONDS.toDays(sprintEndDate.getTime() - currentTimeMillis);
		return remainingDays;
	}

	public static Date getSprintEndDateFromSprintName(String sprintName)
	{
		Document sprintDoc = getAnySprintDoc(sprintName);
		if (sprintDoc == null)
			return null;

		Document infoDoc = (Document) sprintDoc.get(SPRINT_DOC_KEY_SPRINT_INFO);
		String time = infoDoc.getString(SPRINT_INFO_DOC_KEY_END_DATE);

        time = time.replaceAll("/", "-");
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yy hh:mm aaa");
        dateFormat.setDateFormatSymbols(DateFormatSymbols.getInstance(Locale.ENGLISH));
        Date buildCompletedDate = null;
		try
		{
			buildCompletedDate = dateFormat.parse(time);
		}
		catch (ParseException e)
		{
			e.printStackTrace();
		}

		return buildCompletedDate;
	}

	public static Date getSprintEndDateFromSprintDoc(Document sprintDoc)
	{
		Document infoDoc = (Document) sprintDoc.get(SPRINT_DOC_KEY_SPRINT_INFO);
		String time = infoDoc.getString(SPRINT_INFO_DOC_KEY_END_DATE);

        time = time.replaceAll("/", "-");
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yy hh:mm aaa");
        dateFormat.setDateFormatSymbols(DateFormatSymbols.getInstance(Locale.ENGLISH));
        Date sprintEndDate = null;
		try
		{
			sprintEndDate = dateFormat.parse(time);
		}
		catch (ParseException e)
		{
			e.printStackTrace();
		}

		return sprintEndDate;
	}

	public static Date getSprintStartDateFromSprintDoc(Document sprintDoc)
	{
		Document infoDoc = (Document) sprintDoc.get(SPRINT_DOC_KEY_SPRINT_INFO);
		String time = infoDoc.getString(SPRINT_INFO_DOC_KEY_START_DATE);

        time = time.replaceAll("/", "-");
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yy hh:mm aaa");
        dateFormat.setDateFormatSymbols(DateFormatSymbols.getInstance(Locale.ENGLISH));
        Date sprintStartDate = null;
		try
		{
			sprintStartDate = dateFormat.parse(time);
		}
		catch (ParseException e)
		{
			e.printStackTrace();
		}

		return sprintStartDate;
	}

	public static Date getSprintStartDateFromSprintName(String sprintName)
	{
		Document sprintDoc = getAnySprintDoc(sprintName);
		if (sprintDoc == null)
			return null;

		Document infoDoc = (Document) sprintDoc.get(SPRINT_DOC_KEY_SPRINT_INFO);
		String dateString = infoDoc.getString(SPRINT_INFO_DOC_KEY_START_DATE);
        dateString = dateString.replaceAll("/", "-");
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yy hh:mm aaa");
        dateFormat.setDateFormatSymbols(DateFormatSymbols.getInstance(Locale.ENGLISH));
        try {
            return dateFormat.parse(dateString);
        } catch (ParseException e) {
            return null;
        }
    }

	private static Document getAnySprintDoc(String sprintName)
	{
		FindIterable<Document> allSprintDocs = getAllSprintDocs(sprintName);
		if (allSprintDocs != null && allSprintDocs.iterator().hasNext())
			return allSprintDocs.iterator().next();
		return null;
	}

	public static List<Document> getUserDocs(Document agentDataDoc)
	{
		List<Document> res = (List<Document>) agentDataDoc.get(AGENT_DATA_DOC_KEY_USERS);
		if (res == null)
			res = new ArrayList<Document>();
		return res;
	}

	public static List<Document> getSprintsMetaDataDocs(Document agentDataDoc)
	{
		List<Document> res = (List<Document>) agentDataDoc.get(AGENT_DATA_DOC_KEY_SPRINTS_META_DATA);
		if (res == null)
			res = new ArrayList<Document>();
		return res;
	}

	public static List<Document> getSprintDocs(Document agentDataDoc)
	{
		List<Document> res = (List<Document>) agentDataDoc.get(AGENT_DATA_DOC_KEY_SPRINTS);
		if (res == null)
			res = new ArrayList<Document>();
		return res;
	}

	public static List<Document> getJenkinsDocsFromSprintContentDoc(Document sprintContent)
	{
		List<Document> res = (List<Document>) sprintContent.get(CONTENTS_DOC_KEY_JENKINS);
		if (res == null)
			res = new ArrayList<Document>();
		return res;
	}

	public static List<Document> getIssueDocsFromSprintContentDoc(Document sprintContent, boolean completed,
			boolean incompleted, boolean punted)
	{
		List<Document> res = new ArrayList<>();

		Document jiraDoc = (Document) sprintContent.get(CONTENTS_DOC_KEY_JIRA);
		if (jiraDoc == null)
			return res;

		if (completed)
		{
			List<Document> completedIssues = getCompletedIssuesFromJiraDoc(jiraDoc);
			res.addAll(completedIssues);
		}

		if (incompleted)
		{
			List<Document> incompletedIssues = getIncompletedIssuesFromJiraDoc(jiraDoc);
			res.addAll(incompletedIssues);
		}

		if (punted)
		{
			List<Document> puntedIssues = getPuntedIssuesFromJiraDoc(jiraDoc);
			res.addAll(puntedIssues);
		}

		return res;
	}

	private static List<Document> getPuntedIssuesFromJiraDoc(Document jiraDoc)
	{
		List<Document> res = (List<Document>) jiraDoc.get(JIRA_DOC_KEY_ISSUES_PUNTED);
		if (res == null)
			res = new ArrayList<Document>();
		return res;
	}

	public static List<Document> getIncompletedIssues(String sprintName)
	{
		List<Document> incompletedIssuesInSprint = getIssuesInSprint(sprintName, false, true, false);
		return incompletedIssuesInSprint;
	}

	public static List<Document> getCompletedIssues(String sprintName)
	{
		List<Document> incompletedIssuesInSprint = getIssuesInSprint(sprintName, true, false, false);
		return incompletedIssuesInSprint;
	}

	public static List<Document> getPuntedIssues(String sprintName)
	{
		List<Document> incompletedIssuesInSprint = getIssuesInSprint(sprintName, false, false, true);
		return incompletedIssuesInSprint;
	}

	private static List<Document> getIncompletedIssuesFromJiraDoc(Document jiraDoc)
	{
		List<Document> res = (List<Document>) jiraDoc.get(JIRA_DOC_KEY_ISSUES_INCOMPLETED);
		if (res == null)
			res = new ArrayList<Document>();
		return res;
	}

	private static List<Document> getCompletedIssuesFromJiraDoc(Document jiraDoc)
	{
		List<Document> res = (List<Document>) jiraDoc.get(JIRA_DOC_KEY_ISSUES_COMPLETED);
		if (res == null)
			res = new ArrayList<Document>();
		return res;
	}

	public static List<Document> getCommitDocsFromSprintContentDoc(Document sprintContent)
	{
		List<Document> res = (List<Document>) sprintContent.get(CONTENTS_DOC_KEY_GITLAB);
		if (res == null)
			res = new ArrayList<Document>();
		return res;
	}

	public static List<Document> getBambooDocs(Document sprintContent)
	{
		List<Document> res = (List<Document>) sprintContent.get(CONTENTS_DOC_KEY_BAMBOO);
		if (res == null)
			res = new ArrayList<Document>();
		return res;
	}

	public static List<Document> getGitlabDocs(Document sprintContent)
	{
		List<Document> res = (List<Document>) sprintContent.get(CONTENTS_DOC_KEY_GITLAB);
		if (res == null)
			res = new ArrayList<Document>();
		return res;
	}

	public static List<Document> getGithubDocs(Document sprintContent)
	{
		List<Document> res = (List<Document>) sprintContent.get(CONTENTS_DOC_KEY_GITHAB);
		if (res == null)
			res = new ArrayList<Document>();
		return res;
	}

	public static List<Document> getBitbucketDocs(Document sprintContent)
	{
		List<Document> res = (List<Document>) sprintContent.get(CONTENTS_DOC_KEY_BITBUCKET);
		if (res == null)
			res = new ArrayList<Document>();
		return res;
	}

	public static List<Document> getJiraDocsCompletedIssues(Document sprintContent)
	{
		List<Document> res = (List<Document>) sprintContent.get(JIRA_DOC_KEY_ISSUES_COMPLETED);
		if (res == null)
			res = new ArrayList<Document>();
		return res;
	}

	public static List<Document> getJiraDocsIncompletedIssues(Document sprintContent)
	{
		List<Document> res = (List<Document>) sprintContent.get(JIRA_DOC_KEY_ISSUES_INCOMPLETED);
		if (res == null)
			res = new ArrayList<Document>();
		return res;
	}

	public static int getJiraCompletedIssuesCount(Document jiraContent)
	{
		if(jiraContent == null)
			return 0;
		return jiraContent.getInteger(JIRA_DOC_KEY_ISSUES_COMPLETED_COUNT);
	}

	public static int getJiraIncompletedIssuesCount(Document jiraContent)
	{
		if(jiraContent == null)
			return 0;
		return jiraContent.getInteger(JIRA_DOC_KEY_ISSUES_INCOMPLETED_COUNT);
	}

	public static int getJenkinsBuildNumber(Document jenkinsDoc)
	{
		return jenkinsDoc.getInteger(BUILD_DOC_KEY_BUILD_NUMBER);
	}

	public static int getBambooBuildNumber(Document jenkinsDoc)
	{
		return jenkinsDoc.getInteger(BUILD_DOC_KEY_BUILD_NUMBER);
	}

	public static Date getBuildExecutionTimeFromBuildDoc(Document buildDoc)
	{
		Long time = buildDoc.getLong(BUILD_DOC_KEY_EXECUTION_TIME);
		return new Date(time.longValue());
	}

	public static Date getCommitExecutionTimeFromCommitDoc(Document commitDoc)
	{
		String time = commitDoc.getString(COMMIT_DOC_KEY_EXECUTION_TIME);

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		SimpleDateFormat output = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = null;
		try
		{
			Date d = sdf.parse(time);
			String formattedTime = output.format(d);
			date = output.parse(formattedTime);
		}
		catch (ParseException e)
		{
			e.printStackTrace();
		}

//		String time = commitDoc.getString(COMMIT_DOC_KEY_EXECUTION_TIME);
//		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss Z");//.SSSXXX");
//		Date date = null;
//		try
//		{
//			date = dateFormat.parse(time);
//		}
//		catch (ParseException e)
//		{
//			e.printStackTrace();
//		}

		return date;
	}

	public static List<Document> getAllJenkinsDocsFromSprintDoc(Document sprintDoc)
	{
		Document sprintContent = getSprintContentFromSprintDoc(sprintDoc);
		if (sprintContent == null)
			return new ArrayList<Document>();

		List<Document> jenkinsDocs = getJenkinsDocsFromSprintContentDoc(sprintContent);
		return jenkinsDocs;
	}

	public static List<Document> getAllIssueDocsFromSprintDoc(Document sprintDoc, boolean completed,
			boolean incompleted, boolean punted)
	{
		Document sprintContent = getSprintContentFromSprintDoc(sprintDoc);
		if (sprintContent == null)
			return new ArrayList<Document>();

		List<Document> issueDocs = getIssueDocsFromSprintContentDoc(sprintContent, completed, incompleted, punted);
		return issueDocs;
	}

	public static List<Document> getAllCommitDocsFromSprintDoc(Document sprintDoc)
	{
		Document sprintContent = getSprintContentFromSprintDoc(sprintDoc);
		if (sprintContent == null)
			return new ArrayList<Document>();

		List<Document> commitDocs = getCommitDocsFromSprintContentDoc(sprintContent);
		return commitDocs;
	}

	public static boolean isBuildFailed(Document buildDoc)
	{
		String status = buildDoc.getString(BUILD_DOC_KEY_RESULT);
		if (status == null)
			return false;
		return status.equals(BUILD_DOC_VALUE_RESULT_FAILURE);
	}

	public static String getBuildExecutorFromBuildDoc(Document buildDoc)
	{
		String executor = buildDoc.getString(BUILD_DOC_KEY_EXECUTOR);
		if (executor == null)
		{
			return "unknown";
		}
		return executor;
	}

	public static String getCommitExecutorFromCommitDoc(Document commitDoc)
	{
		String executor = commitDoc.getString(COMMIT_DOC_KEY_EXECUTOR);
		if (executor == null)
		{
			return "unknown";
		}

		return executor;
	}

	public static ArrayList<String> getRelatedIssueFromCommitDoc(Document commitDoc)
	{
		ArrayList<String> relatedIssues = (ArrayList<String>)commitDoc.get(COMMIT_DOC_KEY_ISSUE_KEY);
		if (relatedIssues == null)
		{
			return new ArrayList<>();
		}

		return relatedIssues;
	}

	public static int getRelatedBuildFromCommitDoc(Document commitDoc)
	{
		Integer relatedBuild = commitDoc.getInteger(COMMIT_DOC_KEY_BUILD_NUMBER);
		if (relatedBuild == null)
		{
			return Integer.valueOf(-1);
		}

		return relatedBuild;
	}

	public static String getCommitterFromGithubDoc(Document githubDoc)
	{
		Document commitDoc = getCommitDocFromGithubDoc(githubDoc);
		if (commitDoc == null)
		{
			return "unknown";
		}

		Document committerDoc = getCommitterDocFromGithubCommitDoc(commitDoc);
		if (commitDoc == null)
		{
			return "unknown";
		}

		String res = getCommitterNameFromGithubCommitterDoc(committerDoc);
		if (res == null)
		{
			return "unknown";
		}
		
		return res;
	}

	public static String getCommitterFromBitbucketDoc(Document bitbucketDoc)
	{
		Document authorDoc = getAuthorDocFromBitbucketDoc(bitbucketDoc);
		if (authorDoc == null)
		{
			return "unknown";
		}

		Document userDoc = getUserDocFromBitbucketAuthorDoc(authorDoc);
		if (userDoc == null)
		{
			return "unknown";
		}

		String res = getCommiterNameFromBitbucketUserDoc(userDoc);
		if (res == null)
		{
			return "unknown";
		}
		
		return res;
	}

	public static String getExecutionDateFromBitbucketDoc(Document bitbucketDoc)
	{
		String res = bitbucketDoc.getString(BITBUCKET_DOC_KEY_DATE);
		if (res == null)
		{
			return "unknown";
		}
		return res;
	}

	public static String getDescriptionFromBitbucketDoc(Document bitbucketDoc)
	{
		String res = bitbucketDoc.getString(BITBUCKET_DOC_KEY_DESCRIPTION);
		if (res == null)
		{
			return "description not found";
		}
		return res;
	}

	public static String getExecutionDateFromGithubDoc(Document githubDoc)
	{
		Document commitDoc = getCommitDocFromGithubDoc(githubDoc);
		if (commitDoc == null)
		{
			return "unknown";
		}

		Document committerDoc = getCommitterDocFromGithubCommitDoc(commitDoc);
		if (commitDoc == null)
		{
			return "unknown";
		}

		String res = getDateFromGithubCommitterDoc(committerDoc);
		if (res == null)
		{
			return "unknown";
		}
		
		return res;
	}

	public static String getDescriptionFromGithubDoc(Document githubDoc)
	{
		Document commitDoc = getCommitDocFromGithubDoc(githubDoc);
		if (commitDoc == null)
		{
			return "";
		}

		String res = commitDoc.getString(GITHUB_COMMIT_DOC_KEY_MESSAGE);
		if (res == null)
		{
			return "";
		}

		return res;
	}

	public static Document getCommitDocFromGithubDoc(Document githubDoc)
	{
		return ((Document) githubDoc.get(GITHUB_DOC_KEY_COMMIT));
	}

	public static Document getAuthorDocFromBitbucketDoc(Document bitbucketDoc)
	{
		return ((Document) bitbucketDoc.get(BITBUCKET_DOC_KEY_AUTHOR));
	}

	public static String getSnapGlueUserFromDoc(Document doc)
	{
		return doc.getString(DOCUMENT_KEY_SNAPGLUE_USER);
	}
	
	public static Document getUserDocFromBitbucketAuthorDoc(Document bitbucketAuthorDoc)
	{
		return ((Document) bitbucketAuthorDoc.get(BITBUCKET_AUTHOER_DOC_KEY_USER));
	}

	public static String getCommiterNameFromBitbucketUserDoc(Document bitbucketUserDoc)
	{
		return bitbucketUserDoc.getString(BITBUCKET_USER_DOC_KEY_DISPLAY_NAME);
	}

	public static Document getCommitterDocFromGithubCommitDoc(Document githubCommitDoc)
	{
		return ((Document) githubCommitDoc.get(GITHUB_COMMIT_DOC_KEY_COMMITTER));
	}

	public static String getCommitterNameFromGithubCommitterDoc(Document githubCommitterDoc)
	{
		return githubCommitterDoc.getString(GITHUB_COMMITTER_DOC_KEY_NAME);
	}

	public static String getDateFromGithubCommitterDoc(Document githubCommitterDoc)
	{
		return githubCommitterDoc.getString(GITHUB_COMMITTER_DOC_KEY_DATE);
	}

	public static List<Document> getIssueSprintsDataFromIssueDoc(Document issueDoc)
	{
		return ((List<Document>) issueDoc.get(ISSUE_DOC_KEY_SPRINTS_DATA));
	}

    public static List<Document> getIssueStatusesDataFromIssueDoc(Document issueDoc)
    {
        return ((List<Document>) issueDoc.get(ISSUE_DOC_KEY_STATUSES_DATA));
    }

    public static List<Document> getIssueAssigneesDataFromIssueDoc(Document issueDoc)
    {
        return ((List<Document>) issueDoc.get(ISSUE_DOC_KEY_ASSIGNEES_DATA));
    }

	public static String getIssueAssigneeFromIssueDoc(Document issueDoc)
	{
		Document fieldsDoc = ((Document) issueDoc.get(ISSUE_DOC_KEY_FIELDS));
		return getIssueAssigneeFromFieldsDoc(fieldsDoc);
	}

	public static String getIssueAssigneeFromFieldsDoc(Document fieldsDoc)
	{
		Document assigneeDoc = (Document) fieldsDoc.get(FIELDS_DOC_KEY_ASSIGNEE);
		if (assigneeDoc == null)
			return "Unassigned";
		String executor = assigneeDoc.getString(ASSIGNEE_DOC_KEY_NAME);
		if (executor == null)
			return "Unassigned";

		return executor;
	}

	public static String getIssueKeyFromJiraDoc(Document fieldsDoc)
	{
		String key = fieldsDoc.getString(ISSUE_DOC_KEY_KEY);
		if (key == null)
			key = "-1";

		return key;
	}

	public static String getIssueKeyFromIssueDoc(Document issueDoc)
	{
		return issueDoc.getString(ISSUE_DOC_KEY_KEY);
	}

	public static boolean isLastBuildFailed(String sprintName)
	{
		Document lastBuildDoc = getLastBuildDoc(sprintName);
		return isBuildFailed(lastBuildDoc);
	}

	public static Document getLastSprintDoc(String sprintName)
	{
//		db.collection.find().skip(db.collection.count() - N)

		FindIterable<Document> allSprintsSorted = getAllSprintsSorted(sprintName);
		if (allSprintsSorted.iterator().hasNext())
			return allSprintsSorted.iterator().next();

		return null;
	}

	public static Document getLastSprintDocContainingBuild(String sprintName)
	{
		FindIterable<Document> allSprintsSorted = getAllSprintsSorted(sprintName);
		for (Document sprintDoc : allSprintsSorted)
		{
			if (isSprintContainsBuild(sprintDoc))
				return sprintDoc;
		}
		return null;//not found
	}

	private static boolean isSprintContainsBuild(Document sprintDoc)
	{
		List<Document> allJenkinsDocs = getAllJenkinsDocsFromSprintDoc(sprintDoc);
		return !allJenkinsDocs.isEmpty();
	}

	public static FindIterable<Document> getAllSprintsSorted(String sprintName)
	{
		FindIterable<Document> allSprintDocs = getAllSprintDocs(sprintName);
		List<Document> allSprintsList = new ArrayList<>();
		for (Document document : allSprintDocs)
		{
			allSprintsList.add(document);
		}

		Collections.sort(allSprintsList, new DocumentComparator(DOCUMENT_KEY_INSERTION_DATE, Long.class, true));

//		allSprintDocs = allSprintDocs.sort(new Document().append("updated", -1));
		return allSprintDocs;
	}

	public static FindIterable<Document> getAllSprintDocs(String sprintName)
	{
		FindIterable<Document> allSprintDocs = MongoConnectionManager
				.findAllDocsInCollectionByValue(MongoDBCollection.SPRINTS.name(), "sprintInfo.name", sprintName);
		return allSprintDocs;
	}

    public static FindIterable<Document> getAllExternalDataDocs(){
        FindIterable<Document> allExternalDataDocs = MongoConnectionManager
                .findAllDocsInCollection(MongoDBCollection.EXTERNAL_DATA.name());
        return allExternalDataDocs;
    }

	public static Document getLastBuildDoc(String sprintName)
	{
		Document lastSprintDocContainingBuild = getLastSprintDocContainingBuild(sprintName);
		if (lastSprintDocContainingBuild == null)
			return null;

		List<Document> allJenkinsDocs = getAllJenkinsDocsFromSprintDoc(lastSprintDocContainingBuild);
		Document res = null;
		for (Document currentDoc : allJenkinsDocs)
		{
			Long timestamp = currentDoc.getLong("timestamp");
			Date currentDate = new Date(timestamp.longValue());
			if (res == null)
			{
				res = currentDoc;
			}
			else
			{
				Long resTimestamp = res.getLong("timestamp");
				Date resDate = new Date(resTimestamp.longValue());
				if (currentDate.after(resDate))
					res = currentDoc;
			}
		}
		return res;
	}

//	public boolean isLastBuildFailed(String project)
//	{
//		Document lastBuildDoc = MongoConnectionManager.findLastDocInCollection(MongoDBCollection.BUILDS_JENKINS.name(),
//				"timestamp");
//		if (lastBuildDoc != null)
//		{
//			String state = lastBuildDoc.getString("result");
//			return "FAILED".equalsIgnoreCase(state);
//		}
//		return false;
//	}

	public static Document getLastFailedBuild()
	{
		FindIterable<Document> allSprintDocsInCollection = MongoConnectionManager
				.findAllDocsInCollection(MongoDBCollection.SPRINTS.name());

		return getLastFailedBuild(allSprintDocsInCollection);
	}

	private static Document getLastFailedBuild(FindIterable<Document> sprintDocs)
	{
		for (Document sprintDoc : sprintDocs)
		{
			Document lastFailedBuild = getLastFailedBuild(sprintDoc);
			if (lastFailedBuild != null)
				return lastFailedBuild;
		}
		return null;
	}

	private static Document getLastFailedBuild(Document sprintDoc)
	{
		List<Document> allJenkinsDocs = getAllJenkinsDocsFromSprintDoc(sprintDoc);
		Document res = null;
		for (Document currentDoc : allJenkinsDocs)
		{
			String status = currentDoc.getString("result");
			if (status.equals("FAILUE"))
			{
				Long currentTimestamp = currentDoc.getLong("timestamp");
				Date currentDate = new Date(currentTimestamp.longValue());
				if (res == null)
				{
					res = currentDoc;
				}
				else
				{
					Long resTimestamp = currentDoc.getLong("timestamp");
					Date resDate = new Date(resTimestamp.longValue());
					if (currentDate.after(resDate))
						res = currentDoc;
				}
			}
		}
		return res;
	}

	public static Date getTimeOfFirstFailedBuildFromCurrentFailures(String sprintName)
	{
		Document firstFailedBuildFromCurrentFailures = getFirstFailedBuildFromCurrentFailures(sprintName);
		if (firstFailedBuildFromCurrentFailures == null)
			return null;

		Date res = getBuildExecutionTimeFromBuildDoc(firstFailedBuildFromCurrentFailures);
		return res;
	}

	public static Document getFirstFailedBuildFromCurrentFailures(String sprintName)
	{
		Document res = null;

		List<Document> allBuildsSortedByExecutionTime = getAllBuildsSortedByExecutionTime(sprintName);

		for (Document buildDoc : allBuildsSortedByExecutionTime)
		{
			if (isBuildFailed(buildDoc))
				res = buildDoc;
			else
				break;
		}

		return res;
	}

	public static List<Document> getAllBuildsSortedByExecutionTime(String sprintName)
	{
		List<Document> allBuilds = getAllBuilds(sprintName);
		Collections.sort(allBuilds, new DocumentComparator(BUILD_DOC_VALUE_RESULT_NUMBER, Integer.class, true));
		return allBuilds;
	}

	public static List<Document> getAllBuilds(String sprintName)
	{
		List<Document> res = new ArrayList<>();

		FindIterable<Document> allSprints = getAllSprintDocs(sprintName);
		for (Document sprintDoc : allSprints)
		{
			List<Document> allJenkinsDocs = getAllJenkinsDocsFromSprintDoc(sprintDoc);
			res.addAll(allJenkinsDocs);
		}
		return res;
	}

    public static Document getJiraDocFromSprintDoc(Document sprintDoc){
        Document contentDoc = getSprintContentFromSprintDoc(sprintDoc);
        return getJiraContent(contentDoc);
    }

	public static Document getIssueInSprint(String sprintName, String id)
	{
		List<Document> allIssuesInSprint = getIssuesInSprint(sprintName, true, true, true);
		for (Document issueDoc : allIssuesInSprint)
		{
			String title = getIssueTitleFromIssueDoc(issueDoc);
			if (id.equals(title))
				return issueDoc;
		}
		return null;
	}

	/**
	 * Returns the issues in the given sprint according to the given parameters
	 * 
	 * @param sprintName
	 * @param completed
	 * @param incompleted
	 * @param punted
	 * 
	 */
	public static List<Document> getIssuesInSprint(String sprintName, boolean completed, boolean incompleted,
			boolean punted)
	{
		List<Document> res = new ArrayList<>();

		FindIterable<Document> allSprints = getAllSprintDocs(sprintName);
		for (Document sprintDoc : allSprints)
		{
			List<Document> allIssueDocs = getAllIssueDocsFromSprintDoc(sprintDoc, completed, incompleted, punted);
			res.addAll(allIssueDocs);
		}
		return res;
	}

	public static List<Document> getAllCommitsSortedByExecutionTime(String sprintName)
	{
		List<Document> allCommits = getAllCommits(sprintName);
		Collections.sort(allCommits, new DocumentComparator(COMMIT_DOC_KEY_EXECUTION_TIME, String.class, true));
		return allCommits;
	}

	public static List<Document> getAllCommits(String sprintName)
	{
		List<Document> res = new ArrayList<>();

		FindIterable<Document> allSprints = getAllSprintDocs(sprintName);
		for (Document sprintDoc : allSprints)
		{
			List<Document> allJenkinsDocs = getAllCommitDocsFromSprintDoc(sprintDoc);
			res.addAll(allJenkinsDocs);
		}
		return res;
	}

	public static int getNumberOfCommitsSince(String sprintName, Date sprintBrokenTime)
	{
		List<Document> commitsSince = getCommitsSince(sprintName, sprintBrokenTime);
		return commitsSince.size();
	}

	
	private static List<Document> getCommitsSince(String sprintName, Date sprintBrokenTime)
	{
		List<Document> res = new ArrayList<>();
		List<Document> allCommitsSortedByExecutionTime = getAllCommitsSortedByExecutionTime(sprintName);
		for (Document commitDoc : allCommitsSortedByExecutionTime)
		{
			Date commitExecutionTime = getCommitExecutionTimeFromCommitDoc(commitDoc);
			if (sprintBrokenTime.before(commitExecutionTime))
				res.add(commitDoc);
			else
				break;
		}

		return res;
	}

	public static List<Document> getExternalCommits(Date since)
	{
		List<Document> res = new ArrayList<>();

		FindIterable<Document> externalDataDocs = MongoConnectionManager
				.findAllDocsInCollection(MongoDBCollection.EXTERNAL_DATA.name());
		List<Document> sortedList = new ArrayList<>();
		for (Document document : externalDataDocs)
		{
			sortedList.add(document);
		}

		Collections.sort(sortedList, new DocumentComparator(EXTERNAL_DATA_DOC_KEY_INSERT_TIME, Long.class, true));
		for (Document externDataDoc : sortedList)
		{
			Date insertionTime = getInsertionTimeFromExternalDoc(externDataDoc);
			if (insertionTime.before(since))
				continue;

			List<Document> commitDocs = getCommitDocsFromSprintContentDoc(externDataDoc);
			res.addAll(commitDocs);
		}

		return res;
	}

	public static int getLastBuildNumberFromAgentExecutionDataDoc(Document executionDoc)
	{
		Integer res = executionDoc.getInteger(AGENT_EXECUTION_DATA_DOC_KEY_LAST_BUILD_NUMBER);
		return res.intValue();
	}

	public static String getLastSuccessTimeFromAgentExecutionDataDoc(Document executionDoc)
	{
		Long time = executionDoc.getLong(AGENT_EXECUTION_DATA_DOC_KEY_LAST_SUCCESS_TIME);
		return time != null ? String.valueOf(time) : null;
	}

	public static String getIDFromDoc(Document doc)
	{
		return doc.getObjectId(DOCUMENT_KEY_ID).toString();
	}

	public static Document getKPIsFromDoc(Document doc)
	{
		return (Document) doc.get(RULE_DOC_KEY_KPIS);
	}

	public static int getBuildFailureHoursKPIFromDoc(Document doc)
	{
		return doc.getInteger(RULE_DOC_BUILD_FAILURE_KEY_HOURS);
	}

	public static String getIssueIDKPIFromDoc(Document doc)
	{
		return doc.getString(RULE_DOC_ISSUE_STATUS_KEY_ISSUE_ID);
	}

	public static int getDaysBeforeToTriggerAlertKPIFromDoc(Document doc)
	{
		return doc.getInteger(RULE_DOC_ISSUE_STATUS_KEY_DAYS).intValue();
	}

	public static String getDesiredStatusKPIFromDoc(Document doc)
	{
		return doc.getString(RULE_DOC_ISSUE_STATUS_KEY_DESIRED_STATUS);
	}

	public static int getRemainingDaysKPIFromDoc(Document doc)
	{
		return doc.getInteger(RULE_DOC_SPRINT_STATUS_KEY_REMAINING_DAYS).intValue();
	}

	public static int getNumberOfOpenIssuesKPIFromDoc(Document doc)
	{
		return doc.getInteger(RULE_DOC_SPRINT_STATUS_KEY_NUMBER_OF_OPEN_ISSUES).intValue();
	}

	public static String getIssueStatusFromIssueDoc(Document issueDoc)
	{
		Document fieldsDoc = (Document) issueDoc.get(ISSUE_DOC_KEY_FIELDS);
		Document statusDoc = (Document) fieldsDoc.get(FIELDS_DOC_KEY_STATUS);
		String name = statusDoc.getString(STATUS_DOC_KEY_STATUS);
		return name;
	}

    public static Date getIssueCreatedDateFromIssueDoc(Document issueDoc)
    {
        Document fieldsDoc = ((Document) issueDoc.get(ISSUE_DOC_KEY_FIELDS));
		return parseIssueDate(fieldsDoc.getString(FIELDS_DOC_KEY_CREATED));
    }

    public static Date getIssueUpdatedDateFromIssueDoc(Document issueDoc)
    {
        Document fieldsDoc = ((Document) issueDoc.get(ISSUE_DOC_KEY_FIELDS));
        return parseIssueDate(fieldsDoc.getString("updated"));
    }

    public static List<Document> getHistoryDocsFromIssueDocForDateSorted(Document issueDoc, Date forDate, boolean ascending)
    {
    	List<Document> res = new ArrayList<>();
    	
    	List<Document> historyDocs = getHistoryDocsFromIssueDoc(issueDoc);
    	for (Document historyDoc : historyDocs) 
    	{
    		String changeDate = getHistoryDateFromHistoryDoc(historyDoc);
    		Date lastUpdateDate = DateUtils.formatDateStringToDate(changeDate);
    		if(lastUpdateDate == null)
    			continue;

    		long lastUpdateDateLong = lastUpdateDate.getTime();
    		lastUpdateDate = DateUtils.getDateInFormat(lastUpdateDate, null, false);
    		
    		if(lastUpdateDate.equals(forDate))
    		{
    			historyDoc.append(HISTORY_DATA_DOC_KEY_DATE_APPENDED_FOR_SORTING, lastUpdateDateLong);
    			res.add(historyDoc);
    		}
		}
    	
    	
		Collections.sort(res, new DocumentComparator(HISTORY_DATA_DOC_KEY_DATE_APPENDED_FOR_SORTING, Long.class, ascending));

    	
    	return res;
    }
    
    public static List<Document> getHistoryDocsFromIssueDoc(Document issueDoc)
    {
    	Document changeLogDocFromIssueDoc = getChangeLogDocFromIssueDoc(issueDoc);
    	if(changeLogDocFromIssueDoc == null)
    		return new ArrayList<>();
    	
    	List<Document> res = ((List<Document>) changeLogDocFromIssueDoc.get(ISSUE_CHANGE_LOG_DOC_KEY_HISTORIES));
    	if(res == null)
    		return new ArrayList<>();
    	
    	return res;
    }
    
    public static Document getChangeLogDocFromIssueDoc(Document issueDoc)
    {
        Document res = ((Document) issueDoc.get(ISSUE_DOC_KEY_CHANGE_LOG));
        return res;
    }

    
    private static Date parseIssueDate(String dateStr){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        Date date = null;
        try {
            date = dateFormat.parse(dateStr);
        }catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

	public static String getIssueTitleFromIssueDoc(Document issueDoc)
	{
		return issueDoc.getString(ISSUE_DOC_KEY_TITLE);
	}

	public static List<Document> getActiveRules(RuleType ruleType)
	{
		FindIterable<Document> ruleDocs = MongoConnectionManager.findAllDocsInCollectionByValue(
				MongoDBCollection.RULES.name(), MongoUtils.RULE_DOC_KEY_TYPE, ruleType.getTypeName());
		MongoCursor<Document> cursor = ruleDocs.iterator();
		List<Document> activeRules = new ArrayList<>();
		while (cursor.hasNext())
		{
			Document doc = cursor.next();
			boolean enabled = doc.getBoolean(MongoUtils.RULE_DOC_KEY_ENABLED);
			if (enabled)
				activeRules.add(doc);
		}
		return activeRules;
	}

	public static String getRuleID(RuleType ruleType)
	{
		Document doc = getRule(ruleType);
		return MongoUtils.getIDFromDoc(doc);
	}

	public static Document getRule(RuleType ruleType)
	{
		return MongoConnectionManager.findDocumentInCollectionByKeyAndValue(MongoDBCollection.RULES.name(),
				MongoUtils.RULE_DOC_KEY_TYPE, ruleType.getTypeName());
	}

	public static String getExecutorFromHistoryDoc(Document historyDoc) 
	{
		if(historyDoc == null)
			return "";
		
		Document author = (Document)historyDoc.get(HISTORY_DATA_DOC_KEY_AUTHOR);
		String res = author.getString(HISTORY_DATA_AUTHOR_DOC_KEY_DISPLAY_NAME);
		return res;
	}

	public static String getHistoryDateFromHistoryDoc(Document historyDoc) 
	{
		String res = historyDoc.getString(HISTORY_DATA_DOC_KEY_DATE);
		return res;
	}

	public static Pair<String, String> getSprintNameChangedFromHistoryDoc(Document historyDoc) 
	{
		List<Document> items = (List<Document>) historyDoc.get(HISTORY_DATA_DOC_KEY_ITEMS);
		for (Document itemDoc : items) 
		{
			String field = itemDoc.getString(HISTORY_DATA_ITEMS_DOC_KEY_FIELD);
			if(!field.equals("Sprint"))
				continue;
			
			return getFromToPair(itemDoc);
		}
		return null;
	}

	public static Pair<String, String> getStatusChangedFromHistoryDoc(Document historyDoc) 
	{
		List<Document> items = (List<Document>) historyDoc.get(HISTORY_DATA_DOC_KEY_ITEMS);
		for (Document itemDoc : items) 
		{
			String field = itemDoc.getString(HISTORY_DATA_ITEMS_DOC_KEY_FIELD);
			if(!field.equals("status"))
				continue;
			
			return getFromToPair(itemDoc);
		}
		return null;
	}

	public static Pair<String, String> getAssigneeChangedFromHistoryDoc(Document historyDoc) 
	{
		List<Document> items = (List<Document>) historyDoc.get(HISTORY_DATA_DOC_KEY_ITEMS);
		for (Document itemDoc : items) 
		{
			String field = itemDoc.getString(HISTORY_DATA_ITEMS_DOC_KEY_FIELD);
			if(!field.equals("assignee"))
				continue;
			
			return getFromToPair(itemDoc);
		}
		return null;
	}

	private static Pair<String , String> getFromToPair(Document doc)
	{
		String fromString = doc.getString(HISTORY_DATA_ITEMS_DOC_KEY_FROM_STRING);
		if(fromString == null)
			fromString = "";
		String toString = doc.getString(HISTORY_DATA_ITEMS_DOC_KEY_TO_STRING);
		if(toString == null)
			toString = "";
        if (!StringUtils.isEmpty(fromString)){
            String[] fromArray = fromString.split(",");
            fromString = fromArray[fromArray.length-1];
        }
        if (!StringUtils.isEmpty(toString)){
            String[] toArray = toString.split(",");
            toString = toArray[toArray.length-1];
        }
		return new Pair<String, String>(fromString.trim(), toString.trim());
	}
	
	
	public static String getSprintStateFromSprintMetaDataDoc(Document doc) 
	{
		String res = doc.getString(SPRINT_META_DATA_DOC_KEY_STATE);
		return res;
	}
	public static String getSprintNameFromSprintMetaDataDoc(Document doc) 
	{
		String res = doc.getString(SPRINT_META_DATA_DOC_KEY_NAME);
		return res;
	}

}

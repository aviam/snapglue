package com.nigealm.rules.svc;

import javax.jdo.annotations.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import com.mongodb.client.FindIterable;
import com.nigealm.mongodb.MongoConnectionManager;
import com.nigealm.mongodb.MongoConnectionManager.MongoDBCollection;
import com.nigealm.mongodb.MongoUtils;
import com.nigealm.utils.JSONUtils;

@Consumes({ "application/json" })
@Produces({ "application/json" })
@Path("/rulesmanagement")
@Service
public class RulesServiceImpl implements RulesService
{
	public static final String SPRINT_STATUS_MESSAGE = "Project {0}, Sprint {1}: Remaining days: {2}, {3} issues "
			+ "not closed.";
	public static final String SPRINT_TREND_MESSAGE = "Project {0}, Sprint {1}: The Progress trend of the "
			+ "version is negative.\n"
			+ "{2} new issues were added, {3} commits were performed and {4} builds were created regarding external "
			+ "issues.\n" + "Nevertheless, {5} issues were closed. ";
	public static final String ISSUE_STATUS_MESSAGE = "Project {0}, Sprint {1}, Issue {2}: There are {3} days before due date."
			+ " but the issue is not on status {4} (it is in status {5}).";
	public static final String BUILD_FAILED_OCCURRENCE_MESSAGE = "Project ''{0}'', Sprint ''{1}'': Build failure occurred at {2}";
	public static final String BUILD_FAILURE_NOT_FIXED = "Project ''{0}'', Sprint ''{1}'': Build failure occurred at {2} "
			+ "(more than {3} hours) and was not fixed since than.\n{4} commits were performed since this failure.";

	public enum RuleStatus
	{
		Disabled(Boolean.FALSE), Enabled(Boolean.TRUE);

		private Boolean status;

		private RuleStatus(Boolean status)
		{
			this.status = status;
		}

		public Boolean getStatus()
		{
			return this.status;
		}
	}

	public enum RuleUpdatePolicy
	{
		Create(), Update();
	}

	public enum RuleType
	{
		SprintStatus("Version Status", RuleUpdatePolicy.Create), SprintTrend("Version Progress Trend",
				RuleUpdatePolicy.Create), IssueStatus("Issue Status", RuleUpdatePolicy.Create), BuildFailureOccurrence(
						"Build Failure Occurrence", RuleUpdatePolicy.Create), BuildFailureNotFixed(
								"Build Failure Was Not Fixed", RuleUpdatePolicy.Update);

		private String name;
		private RuleUpdatePolicy policy;

		RuleType(String name, RuleUpdatePolicy policy)
		{
			this.name = name;
		}

		public String getTypeName()
		{
			return name;
		}

		public RuleUpdatePolicy getPolicy()
		{
			return policy;
		}

		public static RuleType fromString(String typeName)
		{
			if (typeName != null)
			{
				for (RuleType currRuleType : RuleType.values())
				{
					if (typeName.equalsIgnoreCase(currRuleType.name))
					{
						return currRuleType;
					}
				}
			}
			throw new IllegalArgumentException("No Such Rule named: " + typeName);
		}
	}

	@Override
	@GET
	@Path("/rules")
	@PreAuthorize("hasRole('ROLE_USER')")
	public String getAllRules()
	{
		FindIterable<Document> allRules = MongoConnectionManager
				.findAllDocsInCollection(MongoDBCollection.RULES.name());
		JSONArray res = JSONUtils.convertDocumentListToJSONArray(allRules);

		getAllSprints();
		getAllExternalData();
		return res.toString();
	}

	public String getAllSprints()
	{
		FindIterable<Document> allRules = MongoConnectionManager
				.findAllDocsInCollection(MongoDBCollection.SPRINTS.name());
		JSONArray res = JSONUtils.convertDocumentListToJSONArray(allRules);
		String x = res.toString();
		System.out.println(x);
		return res.toString();
	}

	public String getAllExternalData()
	{
		FindIterable<Document> allRules = MongoConnectionManager
				.findAllDocsInCollection(MongoDBCollection.EXTERNAL_DATA.name());
		JSONArray res = JSONUtils.convertDocumentListToJSONArray(allRules);
		String x = res.toString();
		System.out.println(x);

		return res.toString();
	}

	@Override
	@GET
	@Path("/rules/{id}")
	@PreAuthorize("hasRole('ROLE_USER')")
	public String getRuleById(@PathParam("id") String id)
	{
		Document doc = MongoConnectionManager.findDocumentInCollectionById(MongoDBCollection.RULES.name(), id);
		JSONObject rule = JSONUtils.convertDocumentToJSONObject(doc);
		return rule.toString();
	}

	@PUT
	@Path("/rules/{id}")
	@PreAuthorize("hasRole('ROLE_USER')")
	public void updateRuleById(String rule, @PathParam("id") String id)
	{
		Document doc = Document.parse(rule);
		MongoConnectionManager.updateDocument(MongoDBCollection.RULES.name(), doc, id);
	}


	@Override
	@Transactional
	public void insertDefaultRules()
	{
		// Version Status Rule
		insertSprintStatusRule();

		// Version Progress Trend Rule
		insertSprintProgressTrendRule();

		// Build Failure Occurrence rule
		insertBuildFailureOccurrenceRule();

		// Build Failure Not Fixed Rule
		insertBuildFailureNotFixedRule();

		// Issue Status Rule
		insertIssueStatusRule();
	}

	private void insertSprintProgressTrendRule()
	{
		//first check if this rule exists
		if (doesExistInDB(RuleType.SprintTrend))
			return;

		addDocument(RuleType.SprintTrend, RuleUpdatePolicy.Create, RuleStatus.Disabled, SPRINT_TREND_MESSAGE);
	}

	private void insertIssueStatusRule()
	{
		//first check if this rule exists
		if (doesExistInDB(RuleType.IssueStatus))
			return;

		addDocument(RuleType.IssueStatus, RuleUpdatePolicy.Create, RuleStatus.Enabled, ISSUE_STATUS_MESSAGE);
	}

	private void insertBuildFailureOccurrenceRule()
	{
		//first check if this rule exists
		if (doesExistInDB(RuleType.BuildFailureOccurrence))
			return;

		addDocument(RuleType.BuildFailureOccurrence, RuleUpdatePolicy.Create, RuleStatus.Disabled,
				BUILD_FAILED_OCCURRENCE_MESSAGE);
	}

	private void insertBuildFailureNotFixedRule()
	{
		//first check if this rule exists
		if (doesExistInDB(RuleType.BuildFailureNotFixed))
			return;

		addDocument(RuleType.BuildFailureNotFixed, RuleUpdatePolicy.Update, RuleStatus.Disabled,
				BUILD_FAILURE_NOT_FIXED);
	}

	private void insertSprintStatusRule()
	{
		//first check if this rule exists
		if (doesExistInDB(RuleType.SprintStatus))
			return;

		addDocument(RuleType.SprintStatus, RuleUpdatePolicy.Create, RuleStatus.Disabled, SPRINT_STATUS_MESSAGE);
	}

	private static void addDocument(RuleType type, RuleUpdatePolicy policy, RuleStatus status, String message)
	{
		//create the document
		Document doc = new Document();

		//add properties
		doc.append(MongoUtils.RULE_DOC_KEY_TYPE, type.getTypeName());
		doc.append(MongoUtils.RULE_DOC_KEY_UPDATE_POLICY, policy.name());
		doc.append(MongoUtils.RULE_DOC_KEY_ENABLED, status.getStatus());
		doc.append(MongoUtils.RULE_DOC_KEY_MESSAGE, message);

		//set the kpis
		Document kpis = getKPIsDocumentByRuleType(type);
		doc.append(MongoUtils.RULE_DOC_KEY_KPIS, kpis);

		//save to database
		MongoConnectionManager.addDocument(MongoDBCollection.RULES.name(), doc);
	}

	private static Document getKPIsDocumentByRuleType(RuleType type)
	{
		Document kpis = new Document();

		switch (type)
		{
		case BuildFailureNotFixed:
			kpis.append(MongoUtils.RULE_DOC_BUILD_FAILURE_KEY_HOURS,
					MongoUtils.RULE_DOC_BUILD_FAILURE_DEFAULT_VALUE_HOURS);
			return kpis;

		case BuildFailureOccurrence:
			return kpis;

		case IssueStatus:
			kpis.append(MongoUtils.RULE_DOC_ISSUE_STATUS_KEY_ISSUE_ID,
					MongoUtils.RULE_DOC_ISSUE_STATUS_DEFAULT_VALUE_ISSUE_ID);
			kpis.append(MongoUtils.RULE_DOC_ISSUE_STATUS_KEY_DAYS, MongoUtils.RULE_DOC_ISSUE_STATUS_DEFAULT_VALUE_DAYS);
			kpis.append(MongoUtils.RULE_DOC_ISSUE_STATUS_KEY_DESIRED_STATUS,
					MongoUtils.RULE_DOC_ISSUE_STATUS_DEFAULT_VALUE_DESIRED_STATUS);
			return kpis;

		case SprintStatus:
			kpis.append(MongoUtils.RULE_DOC_SPRINT_STATUS_KEY_REMAINING_DAYS,
					MongoUtils.RULE_DOC_SPRINT_STATUS_DEFAULT_VALUE_REMAINING_DAYS);
			kpis.append(MongoUtils.RULE_DOC_SPRINT_STATUS_KEY_NUMBER_OF_OPEN_ISSUES,
					MongoUtils.RULE_DOC_SPRINT_STATUS_DEFAULT_VALUE_NUMBER_OF_OPEN_ISSUES);
			return kpis;

		case SprintTrend:
			return kpis;

		default:
			return null;
		}
	}

	private static boolean doesExistInDB(RuleType type)
	{
		Document doc = getDocumentByType(type);
		return doc != null;
	}

	private static Document getDocumentByType(RuleType type)
	{
		return MongoConnectionManager.findDocumentInCollectionByKeyAndValue(MongoDBCollection.RULES.name(),
				MongoUtils.RULE_DOC_KEY_TYPE, type.getTypeName());
	}
}

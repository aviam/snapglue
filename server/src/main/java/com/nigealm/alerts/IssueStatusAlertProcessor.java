package com.nigealm.alerts;

import java.util.List;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.springframework.stereotype.Service;
import com.nigealm.alerts.svc.AlertsServiceImpl.AlertStatus;
import com.nigealm.mongodb.MongoUtils;
import com.nigealm.rules.svc.RulesServiceImpl;
import com.nigealm.rules.svc.RulesServiceImpl.RuleType;

@Service
public class IssueStatusAlertProcessor extends AlertProcessor
{

	private static Logger log = Logger.getLogger(IssueStatusAlertProcessor.class);

	@Override
	public void process(String projectName, String sprintName)
	{

		List<Document> activeRules = MongoUtils.getActiveRules(RuleType.IssueStatus);

		if (activeRules.size() > 0)
		{
			for (Document activeRuleDoc : activeRules)
			{
				String ruleID = MongoUtils.getIDFromDoc(activeRuleDoc);

				Document kpis = MongoUtils.getKPIsFromDoc(activeRuleDoc);

				String issueId = MongoUtils.getIssueIDKPIFromDoc(kpis);
				int daysBeforeToTriggerAlert = MongoUtils.getDaysBeforeToTriggerAlertKPIFromDoc(kpis);
				String desiredStatus = MongoUtils.getDesiredStatusKPIFromDoc(kpis);

				Document issueDoc = MongoUtils.getIssueInSprint(sprintName, issueId);
				if (issueDoc == null)
					return;

				String currentIssueStatus = MongoUtils.getIssueStatusFromIssueDoc(issueDoc);

				//lior: I was not able to retrieve the end date of the issue

//				MongoUtils.getSprintInfoDocFromSprintDoc(sprintDoc);
//				Date sprintEndDate = MongoUtils.getSprintEndDateFromSprintName(sprintName);
//				long remainingDays = TimeUnit.MILLISECONDS.toDays(sprintEndDate.getTime() - currentTimeMillis);

				int remainingDays = MongoUtils.getSprintRemainingDays(sprintName);

				if (remainingDays < daysBeforeToTriggerAlert && !desiredStatus.equals(currentIssueStatus))
				{
					String description = buildDescription(RulesServiceImpl.ISSUE_STATUS_MESSAGE, projectName,
							sprintName, issueId, Integer.valueOf(daysBeforeToTriggerAlert),
							desiredStatus, currentIssueStatus);

					alertsService.addOrUpdateAlert(projectName, sprintName, description, System.currentTimeMillis(),
							AlertStatus.ACTIVE.name(), ruleID);
				}
			}
		}
		else
		{
			String ruleID = MongoUtils.getRuleID(RuleType.IssueStatus);
			alertsService.disableAlerts(projectName, sprintName, ruleID);
		}
	}

}

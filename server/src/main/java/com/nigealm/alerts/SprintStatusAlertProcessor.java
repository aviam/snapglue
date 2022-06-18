package com.nigealm.alerts;

import java.util.List;
import org.bson.Document;
import org.springframework.stereotype.Service;
import com.nigealm.alerts.svc.AlertsServiceImpl.AlertStatus;
import com.nigealm.mongodb.MongoUtils;
import com.nigealm.rules.svc.RulesServiceImpl;
import com.nigealm.rules.svc.RulesServiceImpl.RuleType;

/**
 * Created by Gil on 29/05/2015.
 */
@Service
public class SprintStatusAlertProcessor extends AlertProcessor
{

	@Override
	public void process(String projectName, String sprintName)
	{
		List<Document> activeRules = MongoUtils.getActiveRules(RuleType.SprintStatus);

		int remainingDays = MongoUtils.getSprintRemainingDays(sprintName);

		if (activeRules.size() > 0)
		{
			for (Document activeRuleDoc : activeRules)
			{
				String ruleID = MongoUtils.getIDFromDoc(activeRuleDoc);
				Document kpis = MongoUtils.getKPIsFromDoc(activeRuleDoc);

				//kpis
				int remainingDaysKPI = MongoUtils.getRemainingDaysKPIFromDoc(kpis);
				int numberOfOpenIssuesKPI = MongoUtils.getNumberOfOpenIssuesKPIFromDoc(kpis);

				List<Document> incompletedIssues = MongoUtils.getIncompletedIssues(sprintName);
				int numOfOpenIssues = incompletedIssues.size();
				if (remainingDays <= remainingDaysKPI && numOfOpenIssues >= numberOfOpenIssuesKPI)
				{
					long currentTimeMillis = System.currentTimeMillis();

					String description = buildDescription(RulesServiceImpl.SPRINT_STATUS_MESSAGE, projectName,
							sprintName, remainingDays,
							numOfOpenIssues);

					alertsService.addOrUpdateAlert(projectName, sprintName, description, currentTimeMillis,
							AlertStatus.ACTIVE.name(), ruleID);
				}
			}
		}
		else
		{
			String ruleID = MongoUtils.getRuleID(RuleType.SprintStatus);
			alertsService.disableAlerts(projectName, sprintName, ruleID);
		}
	}
}

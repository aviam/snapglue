package com.nigealm.alerts;

import java.util.Date;
import java.util.List;
import org.bson.Document;
import org.springframework.stereotype.Service;
import com.nigealm.alerts.svc.AlertsServiceImpl.AlertStatus;
import com.nigealm.common.utils.Tracer;
import com.nigealm.mongodb.MongoUtils;
import com.nigealm.rules.svc.RulesServiceImpl;
import com.nigealm.rules.svc.RulesServiceImpl.RuleType;

@Service
public class BuildFailureAlertProcessor extends AlertProcessor
{

	private static Tracer tracer = new Tracer(BuildFailureAlertProcessor.class);

	private static final long HOURS_TO_MILLIS = 60 * 60 * 1000;

	@Override
	public void process(String projectName, String sprintName)
	{
		processBuildFailureOccurrenceRule(projectName, sprintName);
	}

	private void processBuildFailureOccurrenceRule(String projectName, String sprintName)
	{
		List<Document> activeRules = MongoUtils.getActiveRules(RuleType.BuildFailureOccurrence);

		if (activeRules.size() > 0)
		{
			for (Document activeRuleDoc : activeRules)
			{

				String ruleID = MongoUtils.getIDFromDoc(activeRuleDoc);

				Document lastBuildDoc = MongoUtils.getLastBuildDoc(sprintName);

				if (MongoUtils.isBuildFailed(lastBuildDoc))
				{
					Date lastFailedBuildTime = MongoUtils.getBuildExecutionTimeFromBuildDoc(lastBuildDoc);
					String description = buildDescription(RulesServiceImpl.BUILD_FAILED_OCCURRENCE_MESSAGE, projectName,
							sprintName, lastFailedBuildTime.toString());
					alertsService.addAlert(projectName, sprintName, description, System.currentTimeMillis(),
							AlertStatus.ACTIVE.name(), ruleID);

					// Check the rule not fixed failure
					processBuildFailureNotFixed(projectName, sprintName);
				}
				else
				{
					alertsService.disableAlerts(projectName, sprintName, ruleID);

					String tempRuleID = MongoUtils.getRuleID(RuleType.BuildFailureNotFixed);
					alertsService.disableAlerts(projectName, sprintName, tempRuleID);
				}
			}
		}
		else
		{
			String ruleID = MongoUtils.getRuleID(RuleType.BuildFailureOccurrence);
			alertsService.disableAlerts(projectName, sprintName, ruleID);

			ruleID = MongoUtils.getRuleID(RuleType.BuildFailureNotFixed);
			alertsService.disableAlerts(projectName, sprintName, ruleID);
		}
	}

	private void processBuildFailureNotFixed(String projectName, String sprintName)
	{
		List<Document> activeRules = MongoUtils.getActiveRules(RuleType.BuildFailureNotFixed);

		if (activeRules.size() > 0)
		{
			for (Document activeRuleDoc : activeRules)
			{
				String ruleID = MongoUtils.getIDFromDoc(activeRuleDoc);
				Document kpis = MongoUtils.getKPIsFromDoc(activeRuleDoc);
				int hours = MongoUtils.getBuildFailureHoursKPIFromDoc(kpis);

				Date sprintBrokenTime = MongoUtils.getTimeOfFirstFailedBuildFromCurrentFailures(sprintName);
				int numOfCommitsSinceBrokenTime = 0;
				if (sprintBrokenTime != null)
					numOfCommitsSinceBrokenTime = MongoUtils.getNumberOfCommitsSince(sprintName, sprintBrokenTime);
				else
					return;

				long currentTimeMillis = System.currentTimeMillis();
				if (currentTimeMillis - sprintBrokenTime.getTime() > hours * HOURS_TO_MILLIS)
				{
					String description = buildDescription(RulesServiceImpl.BUILD_FAILURE_NOT_FIXED, projectName,
							sprintName, sprintBrokenTime.toString(), hours, numOfCommitsSinceBrokenTime);
					alertsService.addOrUpdateAlert(projectName, sprintName, description, currentTimeMillis,
							AlertStatus.ACTIVE.name(), ruleID);
				}
				else
				{
					alertsService.disableAlerts(projectName, sprintName, ruleID);
				}
			}
		}
		else
		{
			String ruleID = MongoUtils.getRuleID(RuleType.BuildFailureNotFixed);
			alertsService.disableAlerts(projectName, sprintName, ruleID);
		}
	}
}

package com.nigealm.alerts;

import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.springframework.stereotype.Service;
import com.nigealm.mongodb.MongoUtils;
import com.nigealm.rules.svc.RulesServiceImpl.RuleType;

/**
 * Created by Gil on 09/06/2015.
 */

@Service
public class SprintTrendAlertProcessor extends AlertProcessor
{

	private static Logger log = Logger.getLogger(SprintTrendAlertProcessor.class);

	@Override
	public void process(String projectName, String sprintName)
	{
		List<Document> activeRules = MongoUtils.getActiveRules(RuleType.SprintTrend);

		if (activeRules.size() > 0)
		{
			for (Document activeRuleDoc : activeRules)
			{
				String ruleID = MongoUtils.getIDFromDoc(activeRuleDoc);
				Document kpis = MongoUtils.getKPIsFromDoc(activeRuleDoc);

				//kpis
				int remainingDaysKPI = MongoUtils.getRemainingDaysKPIFromDoc(kpis);

				List<Document> puntedIssues = MongoUtils.getPuntedIssues(sprintName);
				int numberOfPuntedIssues = puntedIssues.size();

				Date sprintStartTime = MongoUtils.getSprintStartDateFromSprintName(sprintName);
				List<Document> externalCommits = MongoUtils.getExternalCommits(sprintStartTime);
				int numOfExternalCommits = externalCommits.size();

//				int numOfNewIssues = issuesTool.getNumberOfCreatedIssuesSince(projectId, version, startDate, sprintName);
//				int numOfExternalCommits = scmTool.getNumberOfExternalCommits(projectConfigurationDTO);

			}
		}
		else
		{
			String ruleID = MongoUtils.getRuleID(RuleType.SprintTrend);
			alertsService.disableAlerts(projectName, sprintName, ruleID);
		}

//		ProjectMetadataDTO projectMetadataDTO = projectMetadataService.getProjectMetadata(projectId, version,
//				sprintName);
//		ProjectConfigurationDTO projectConfigurationDTO = projectConfigurationService.getProjectConfiguration(projectId,
//				version, sprintName);
//		Date startDate = new Date(projectMetadataDTO.getSprintStartDate());
//		IssuesTool issuesTool = projectMetadataService.getIssuesTool();
//		ScmTool scmTool = projectMetadataService.getScmTool();
//
//		int numOfNewIssues = issuesTool.getNumberOfCreatedIssuesSince(projectId, version, startDate, sprintName);
//		int numOfExternalCommits = scmTool.getNumberOfExternalCommits(projectConfigurationDTO);
//
//		int sprintSize = issuesTool.getSprintPlanIssues(projectId, version, sprintName).size();
//
//		if (sprintSize == 0)
//		{
//			log.warn("Sprint size is 0. Project Trend Rule will not be processed");
//			return;
//		}
//
//		long currentTimeInMillis = System.currentTimeMillis();
//		double sprintCommitsRatio = numOfExternalCommits / sprintSize;
//
//		long sprintDuration = TimeUnit.MILLISECONDS
//				.toDays(projectMetadataDTO.getSprintEndDate() - projectMetadataDTO.getSprintStartDate());
//		long timeLeftInDays = TimeUnit.MILLISECONDS.toDays(projectMetadataDTO.getSprintEndDate() - currentTimeInMillis);
//		double timeRatio = timeLeftInDays / sprintDuration;
//
//		for (RuleDTO ruleDTO : ruleDTOList)
//		{
//			if (ruleDTO == null)
//			{
//				continue;
//			}
//
//			String[] params = ruleDTO.getParams();
//			if (params.length != 1)
//			{
//				log.error("Wrong number of parameters to process Project Trend rule.");
//				return;
//			}
//
//			List<String> closedStatuses = ArrayUtils.asList(StringUtils.split(params[0], ","));
//			int numOfClosedIssues = issuesTool.getNumberOfClosedIssue(projectId, version, sprintName, closedStatuses);
//			double closedIssuesRation = numOfClosedIssues / (numOfNewIssues + sprintSize);
//
//			if (sprintCommitsRatio > 0.3 || (closedIssuesRation < 0.5 && timeRatio > 0 / 5))
//			{
//				alertsService.addAlert(new AlertVO(projectId,
//						version, sprintName, MessageFormat.format(ruleDTO.getMessage(), numOfNewIssues,
//								numOfExternalCommits, numOfClosedIssues),
//						currentTimeInMillis, AlertDTO.AlertStatus.ACTIVE, ruleDTO.getId()));
//			}
//
//		}
	}
}

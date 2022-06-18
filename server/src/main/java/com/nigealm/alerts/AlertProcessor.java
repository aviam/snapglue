package com.nigealm.alerts;

import java.text.MessageFormat;
import org.springframework.beans.factory.annotation.Autowired;
import com.nigealm.alerts.svc.AlertsService;
import com.nigealm.rules.svc.RulesService;

public abstract class AlertProcessor
{

	protected RulesService rulesService;

	@Autowired
	protected AlertsService alertsService;

	public abstract void process(String projectName, String sprintName);

	protected String buildDescription(String desc, Object... params)
	{
		return MessageFormat.format(desc, params);
	}

	protected void disableRuleAlerts(String ruleID, String projectName, AlertsService alertsService, String sprintName)
	{
		alertsService.disableAlerts(projectName, sprintName, ruleID);
	}
}

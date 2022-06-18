package com.nigealm.alerts.svc;

public interface AlertsService
{
	
	String getActiveAlerts();
	void addAlert(String projectId, String sprintName, String description, long currentTimeMillis, String status,
			String ruleID);

	void disableAlerts(String project, String sprintName, String ruleID);
	int getActiveAlertsCount();

	void addOrUpdateAlert(String projectName, String sprintName, String description, long currentTimeMillis,
			String status, String ruleID);
}

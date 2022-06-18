package com.nigealm.alerts.svc.engine;

/**
 * Engine for computing alerts
 * 
 * @author zvikai
 *
 */
public interface AlertsEngine
{

	void computeAndTriggerAlerts(String projectName, String sprintName);

}

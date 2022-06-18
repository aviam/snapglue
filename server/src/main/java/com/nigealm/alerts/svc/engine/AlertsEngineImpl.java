package com.nigealm.alerts.svc.engine;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.nigealm.alerts.AlertProcessor;

@Service
public class AlertsEngineImpl implements AlertsEngine
{

	@Autowired
	private List<AlertProcessor> alerts;

	@Override
	public void computeAndTriggerAlerts(String projectName, String sprintName)
	{
		for (AlertProcessor alertProcessor : alerts)
		{
			alertProcessor.process(projectName, sprintName);
		}
	}

}

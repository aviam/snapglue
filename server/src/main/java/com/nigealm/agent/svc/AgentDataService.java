package com.nigealm.agent.svc;

import com.nigealm.agent.svc.AgentDataServiceImpl.AgentLastExecutionData;

public interface AgentDataService{

	AgentLastExecutionData getAgentLastExecutionData(String configID);

	void updateAgentExecutionData(int lastBuildNumber, String configID, boolean updateExecutionDate);
}

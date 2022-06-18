package com.nigealm.rules.svc;

import javax.ws.rs.PathParam;

public interface RulesService {
	
	String getAllRules();
	String getRuleById(@PathParam("id") String id);

	void updateRuleById(String rule, @PathParam("id") String id);

	void insertDefaultRules();

}

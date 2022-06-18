package com.nigealm.agent.impl;

/**
 * Created by Gil on 06/04/2016.
 */
public class AgentDataCollectionException extends Exception {
    public AgentDataCollectionException(String confId, String toolName, Throwable cause){
        super(confId + ": Data collection failed in tool: " + toolName + " Data will not be saved!" , cause);
    }

    public AgentDataCollectionException(String confId, String toolName){
        super(confId + ": Data collection failed in tool: " + toolName + " Data will not be saved!");
    }
}

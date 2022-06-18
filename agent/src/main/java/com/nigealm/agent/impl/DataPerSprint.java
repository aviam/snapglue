package com.nigealm.agent.impl;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Gil on 25/12/2015.
 * Data holder for each retrieved data from a tool
 */
public class DataPerSprint {

    Map<String, JSONArray> data;

    public DataPerSprint() {
        data = new HashMap<>();
    }

    public void addData(String sprintName, JSONObject obj){
        JSONArray dataArray = data.get(sprintName);
        if (dataArray == null){
            dataArray = new JSONArray();
            data.put(sprintName, dataArray);
        }
        dataArray.put(obj);
    }

    public void mergeData(DataPerSprint otherData){
        Map<String, JSONArray> otherDataMap = otherData.getDataPerSprint();
        for (Map.Entry<String, JSONArray> dataRecord : otherDataMap.entrySet()) {
            String sprintName = dataRecord.getKey();
            JSONArray dataArray = data.get(sprintName);

            if (dataArray == null) {
                data.put(sprintName, dataRecord.getValue());
            }else {
                JSONArray otherDataArray = dataRecord.getValue();
                for (int i = 0; i < otherDataArray.length(); i++) {
                    try {
                        dataArray.put(otherDataArray.get(i));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public Map<String, JSONArray> getDataPerSprint(){
        return data;
    }
}

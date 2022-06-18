package com.nigealm.agent.users;

import com.nigealm.agent.impl.JsonUtils;
import com.nigealm.common.utils.Tracer;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class UserManager {

    private final static Tracer tracer = new Tracer(UserManager.class);

    List<User> users;
    private List<String> ignoreList;

    public UserManager() {
        users = new CopyOnWriteArrayList<>();
        ignoreList = new CopyOnWriteArrayList<>();
    }

    public synchronized boolean addToolToUserByEmail(String email, String tool, String username) {
        for (User currUser : users) {
            if (currUser.matchByEmail(email)) {
                if (StringUtils.isEmpty(username)){
                    currUser.addUserName(tool, email);
                }else{
                    currUser.addUserName(tool, username);
                }
                return true;
            }
        }
        return false;
    }

    public synchronized boolean addToolToUserByFullName(String fullName, String tool, String username) {
        for (User currUser : users) {
            if (currUser.matchByFullName(fullName)) {
                if (StringUtils.isEmpty(username)){
                    currUser.addUserName(tool, fullName);
                }else{
                    currUser.addUserName(tool, username);
                }
                return true;
            }
        }
        return false;
    }

    public synchronized boolean addToolToUserByExistingUserName(String tool, String username) {
        for (User currUser : users) {
            if (currUser.matchByUserName(username) || currUser.matchByFullName(username)) {
                currUser.addUserName(tool, username);
                return true;
            }
        }
        return false;
    }

    public synchronized void createNewUser(String fullName, String email, String userName) {
        users.add(new User(fullName, email, userName));
    }

    public synchronized boolean addToolToUserByEmailOrFullNameOrUserName(String toolName, String toolUserName, String
            email, String
            fullName) {
        if (ignoreList.contains(toolUserName)) {
            return false;
        }

        boolean isAddedSuccessfully = addToolToUserByEmail(email, toolName, toolUserName);
        if (!isAddedSuccessfully) {
            isAddedSuccessfully = addToolToUserByFullName(fullName, toolName, toolUserName);
        }
        if (!isAddedSuccessfully) {
            isAddedSuccessfully = addToolToUserByExistingUserName(toolName, toolUserName);
        }
        if (!isAddedSuccessfully) {
            addUserToIgnoreList(toolUserName);
            tracer.warn("Failed to add tool username for tool:" + toolName + " with user: " + toolUserName);
        }
        return isAddedSuccessfully;
    }

    private void addUserToIgnoreList(String toolUserName) {
        ignoreList.add(toolUserName);
    }

    public User matchUserByEmailOrFullNameOrUserName(String username, String email, String
            fullName) {

        for (User currUser : users) {
            if (currUser.matchByFullName(fullName) || currUser.matchByFullName(username)) {
                return currUser;
            }
        }

        for (User currUser : users) {
            if (currUser.matchByEmail(email)) {
                return currUser;
            }
        }

        for (User currUser : users) {
            if (currUser.matchByUserName(username) || currUser.matchByUserName(fullName)) {
                return currUser;
            }
        }

        return null;
    }

    public synchronized JSONArray getUsersListAsJsonArray() {
        JSONArray usersList = new JSONArray();
        for (User currUser : users) {
            try {
                usersList.put(new JSONObject(JsonUtils.toJson(currUser)));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return usersList;
    }

}

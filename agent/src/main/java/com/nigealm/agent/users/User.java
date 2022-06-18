package com.nigealm.agent.users;

import java.util.HashMap;
import java.util.Map;

public class User {
    String fullName;
    String userName;
    String email;
    Map<String, String> usernamePerTool;

    public User() {
        fullName = "";
        email = "";
        usernamePerTool = new HashMap<>();
    }

    public User(String name) {
        this();
        fullName = name;
    }

    public User(String name, String email, String userName) {
        this(name);
        this.email = email;
        this.userName = userName;
    }

    public void addUserName(String tool, String username) {
        usernamePerTool.put(tool, username);
    }

    public boolean matchByFullName(String fullName) {
        return this.fullName.equalsIgnoreCase(fullName);
    }

    public boolean matchByEmail(String otherEmail) {
        return (email != null && (email.equalsIgnoreCase(otherEmail) || email.toLowerCase().startsWith(otherEmail)));
    }

    public boolean matchByUserName(String username) {
        for (String currUserName : usernamePerTool.values()) {
            if (currUserName.equals(username)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        String retVale = "{";
        retVale += "fullName:" + fullName + ",";
        retVale += "email:" + email + ",";
        retVale += "userNamePerTool:";
        retVale += "{";
        for (Map.Entry<String, String> currEntry : usernamePerTool.entrySet()) {
            retVale += currEntry.getKey() + ":" + currEntry.getValue() + ",";
        }
        // remove last comma
        retVale = retVale.substring(0, retVale.length() - 1);
        retVale += "}}";
        return retVale;
    }

    public String getUserName() {
        return userName;
    }

    public String getFullName(){
        return fullName;
    }
}

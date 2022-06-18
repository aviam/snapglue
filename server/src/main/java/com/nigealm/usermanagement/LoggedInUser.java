package com.nigealm.usermanagement;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class LoggedInUser {
    private String username;

    protected LoggedInUser() {
        // protected - to avoid creation of empty entity
    }

    public LoggedInUser(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}

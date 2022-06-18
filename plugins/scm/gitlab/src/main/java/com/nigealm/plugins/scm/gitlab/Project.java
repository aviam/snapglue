package com.nigealm.plugins.scm.gitlab;

public class Project{

    private String id;
    private String name;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString(){
        String val = "Project:\n";
        val += "\t id: " + id + "\n";
        val += "\t name: " + name + "\n";
        return val;
    }
}

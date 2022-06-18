package com.nigealm.plugins.scm.gitlab;

/**
 * Created by Gil on 25/04/2015.
 */
public class CommitDiff {
    private String diff;
    private String new_path;
    private String old_path;
    private String a_mode;
    private String b_mode;
    private boolean new_file;
    private boolean renamed_file;
    private boolean deleted_file;

    @Override
    public String toString(){
        String val="\t\t\tCommit Diff:\n";
        val += "\t\t\t\toldPath: " + old_path + "\n";
        val += "\t\t\t\tnewPath: " + new_path + "\n";
        val += "\t\t\t\taMode: " + a_mode + "\n";
        val += "\t\t\t\tbMode: " + b_mode + "\n";
        val += "\t\t\t\tnewFile: " + new_file + "\n";
        val += "\t\t\t\trenamedFile: " + renamed_file + "\n";
        val += "\t\t\t\tdeletedFile: " + deleted_file + "\n";
        return val;
    }

    public String getDiff() {
        return diff;
    }

    public void setDiff(String diff) {
        this.diff = diff;
    }

    public String getNew_path() {
        return new_path;
    }

    public void setNew_path(String new_path) {
        this.new_path = new_path;
    }

    public String getOld_path() {
        return old_path;
    }

    public void setOld_path(String old_path) {
        this.old_path = old_path;
    }

    public String getA_mode() {
        return a_mode;
    }

    public void setA_mode(String a_mode) {
        this.a_mode = a_mode;
    }

    public String getB_mode() {
        return b_mode;
    }

    public void setB_mode(String b_mode) {
        this.b_mode = b_mode;
    }

    public boolean isNew_file() {
        return new_file;
    }

    public void setNew_file(boolean new_file) {
        this.new_file = new_file;
    }

    public boolean isRenamed_file() {
        return renamed_file;
    }

    public void setRenamed_file(boolean renamed_file) {
        this.renamed_file = renamed_file;
    }

    public boolean isDeleted_file() {
        return deleted_file;
    }

    public void setDeleted_file(boolean deleted_file) {
        this.deleted_file = deleted_file;
    }
}

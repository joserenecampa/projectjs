package com.timesheet;

public class Item {
    private String id;
    private String group;
    private Boolean open;
    private String typeOfWork;
    private String start;
    private String end;
    private Boolean persist;
    private int orderDb;
    private String type;
    private String className;
    private String[] nestedGroups;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Boolean getOpen() {
        return open;
    }

    public void setOpen(Boolean open) {
        this.open = open;
    }

    public String getTypeOfWork() {
        return typeOfWork;
    }

    public void setTypeOfWork(String typeOfWork) {
        this.typeOfWork = typeOfWork;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public Boolean getPersist() {
        return persist;
    }

    public void setPersist(Boolean persist) {
        this.persist = persist;
    }

    public int getOrderDb() {
        return orderDb;
    }

    public void setOrderDb(int orderDb) {
        this.orderDb = orderDb;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String[] getNestedGroups() {
        return nestedGroups;
    }

    public void setNestedGroups(String[] nestedGroups) {
        this.nestedGroups = nestedGroups;
    }

}
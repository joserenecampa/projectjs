package com.timesheet.importer;

import java.util.Set;
import java.util.UUID;

public class Item {
    private String id = UUID.randomUUID().toString();
    private String group;
    private Boolean open;
    private Boolean checked;
    private String typeOfWork;
    private String start;
    private String end;
    private Boolean persist;
    private int orderDb;
    private int order;
    private String type;
    private String className;
    private String employeeName;
    private String content;
    private String nestedInGroup;
    private Set<String> nestedGroups;

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

    public Set<String> getNestedGroups() {
        return nestedGroups;
    }

    public void setNestedGroups(Set<String> nestedGroups) {
        this.nestedGroups = nestedGroups;
    }

	public String getEmployeeName() {
		return employeeName;
	}

	public void setEmployeeName(String employeeName) {
		this.employeeName = employeeName;
	}

    public Boolean getChecked() {
        return checked;
    }

    public void setChecked(Boolean checked) {
        this.checked = checked;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getNestedInGroup() {
        return nestedInGroup;
    }

    public void setNestedInGroup(String nestedInGroup) {
        this.nestedInGroup = nestedInGroup;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
    
}
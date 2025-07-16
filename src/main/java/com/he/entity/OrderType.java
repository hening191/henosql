package com.he.entity;

public enum OrderType {
    ASC("asc"),
    DESC("desc");

    private String type;

    OrderType(String type) {
        this.type = type;
    }

    public String getType(){
        return type;
    }
}

package com.example.teamgame28.model;

public class TaskCategory {

    private String id;
    private String userId;
    private String name;
    private String color;

    public TaskCategory(){}

    public TaskCategory(String id, String userId, String name, String color) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @Override
    public String toString() {
        return "TaskCategory{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", name='" + name + '\'' +
                ", color='" + color + '\'' +
                '}';
    }


}

package com.example.myapplication.models;

public class Category {
    private String id;
    private String category_name;
    private String user_id;

    public Category() {
        // Default constructor for Firebase
    }

    public Category(String category_name, String user_id) {
        this.category_name = category_name;
        this.user_id = user_id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategory_name() {
        return category_name;
    }

    public void setCategory_name(String category_name) {
        this.category_name = category_name;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }
}

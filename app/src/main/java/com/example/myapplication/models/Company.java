package com.example.myapplication.models;

import java.io.Serializable;

public class Company implements Serializable {
    private String id;
    private String company_name;
    private String phone;
    private String address;
    private String note;
    private String user_id;
    private Long created_at;
    private Long updated_at;
    private Integer items_count;

    public Company() {
        // Required for Firestore
    }

    public Company(String company_name, String phone, String address, String note) {
        this.company_name = company_name;
        this.phone = phone;
        this.address = address;
        this.note = note;
        this.items_count = 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCompany_name() {
        return company_name != null ? company_name : "";
    }

    public void setCompany_name(String company_name) {
        this.company_name = company_name;
    }

    public String getPhone() {
        return phone != null ? phone : "";
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address != null ? address : "";
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getNote() {
        return note != null ? note : "";
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public Long getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Long created_at) {
        this.created_at = created_at;
    }

    public Long getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(Long updated_at) {
        this.updated_at = updated_at;
    }

    public Integer getItems_count() {
        return items_count != null ? items_count : 0;
    }

    public void setItems_count(Integer items_count) {
        this.items_count = items_count;
    }
}

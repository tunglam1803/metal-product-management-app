package com.example.myapplication.models;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CompanyProduct implements Serializable {
    private String id;
    private String company_id;
    private String company_name;
    private String product_id;
    private String product_code;
    private String product_name;
    private String image_url;
    private Double custom_price;
    private Double standard_price;
    private String note;
    private Long updated_at;
    private String user_id;

    public CompanyProduct() {
        // Required for Firestore
    }

    public CompanyProduct(String company_id, String product_id, String product_code, String product_name,
                          Double custom_price, Double standard_price, String note) {
        this.company_id = company_id;
        this.product_id = product_id;
        this.product_code = product_code;
        this.product_name = product_name;
        this.custom_price = custom_price;
        this.standard_price = standard_price;
        this.note = note;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCompany_id() {
        return company_id;
    }

    public void setCompany_id(String company_id) {
        this.company_id = company_id;
    }

    public String getCompany_name() {
        return company_name != null ? company_name : "";
    }

    public void setCompany_name(String company_name) {
        this.company_name = company_name;
    }

    public String getProduct_id() {
        return product_id;
    }

    public void setProduct_id(String product_id) {
        this.product_id = product_id;
    }

    public String getProduct_code() {
        return product_code != null ? product_code : "";
    }

    public void setProduct_code(String product_code) {
        this.product_code = product_code;
    }

    public String getProduct_name() {
        return product_name != null ? product_name : "";
    }

    public void setProduct_name(String product_name) {
        this.product_name = product_name;
    }

    public String getImage_url() {
        return image_url;
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
    }

    public Double getCustom_price() {
        return custom_price != null ? custom_price : 0.0;
    }

    public void setCustom_price(Double custom_price) {
        this.custom_price = custom_price;
    }

    public Double getStandard_price() {
        return standard_price != null ? standard_price : 0.0;
    }

    public void setStandard_price(Double standard_price) {
        this.standard_price = standard_price;
    }

    public String getNote() {
        return note != null ? note : "";
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Long getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(Long updated_at) {
        this.updated_at = updated_at;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getFormattedCustomPrice() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
        return formatter.format(getCustom_price()) + " đ";
    }

    public String getFormattedStandardPrice() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
        return formatter.format(getStandard_price()) + " đ";
    }
}

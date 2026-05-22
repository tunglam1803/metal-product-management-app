package com.example.myapplication.models;

public class Product {
    private String id;
    private String product_code;
    private String product_name;
    private Double import_price;
    private Double sell_price;
    private String image_url;
    private Long created_at;
    private Long updated_at;
    private String category_id;
    private Integer stock_quantity;
    private Boolean is_bundle;
    private String note;

    public Product() {
        // Default constructor required for calls to
        // DataSnapshot.getValue(Product.class)
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProduct_code() {
        return product_code;
    }

    public void setProduct_code(String product_code) {
        this.product_code = product_code;
    }

    public String getProduct_name() {
        return product_name;
    }

    public void setProduct_name(String product_name) {
        this.product_name = product_name;
    }

    public Double getImport_price() {
        return import_price != null ? import_price : 0.0;
    }

    public void setImport_price(Double import_price) {
        this.import_price = import_price;
    }

    public Double getSell_price() {
        return sell_price != null ? sell_price : 0.0;
    }

    public void setSell_price(Double sell_price) {
        this.sell_price = sell_price;
    }

    public String getImage_url() {
        return image_url;
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
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

    public String getCategory_id() {
        return category_id;
    }

    public void setCategory_id(String category_id) {
        this.category_id = category_id;
    }

    public Integer getStock_quantity() {
        return stock_quantity != null ? stock_quantity : 0;
    }

    public void setStock_quantity(Integer stock_quantity) {
        this.stock_quantity = stock_quantity;
    }

    public Boolean getIs_bundle() {
        return is_bundle != null ? is_bundle : false;
    }

    public void setIs_bundle(Boolean is_bundle) {
        this.is_bundle = is_bundle;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}

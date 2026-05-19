package com.example.myapplication.models;

public class PriceHistory {
    private String id;
    private Double old_import_price;
    private Double new_import_price;
    private Double old_sell_price;
    private Double new_sell_price;
    private Long changed_at;
    private String note;

    public PriceHistory() {
    }

    public PriceHistory(Double old_import_price, Double new_import_price, Double old_sell_price, Double new_sell_price,
            Long changed_at, String note) {
        this.old_import_price = old_import_price;
        this.new_import_price = new_import_price;
        this.old_sell_price = old_sell_price;
        this.new_sell_price = new_sell_price;
        this.changed_at = changed_at;
        this.note = note;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Double getOld_import_price() {
        return old_import_price != null ? old_import_price : 0.0;
    }

    public void setOld_import_price(Double old_import_price) {
        this.old_import_price = old_import_price;
    }

    public Double getNew_import_price() {
        return new_import_price != null ? new_import_price : 0.0;
    }

    public void setNew_import_price(Double new_import_price) {
        this.new_import_price = new_import_price;
    }

    public Double getOld_sell_price() {
        return old_sell_price != null ? old_sell_price : 0.0;
    }

    public void setOld_sell_price(Double old_sell_price) {
        this.old_sell_price = old_sell_price;
    }

    public Double getNew_sell_price() {
        return new_sell_price != null ? new_sell_price : 0.0;
    }

    public void setNew_sell_price(Double new_sell_price) {
        this.new_sell_price = new_sell_price;
    }

    public Long getChanged_at() {
        return changed_at;
    }

    public void setChanged_at(Long changed_at) {
        this.changed_at = changed_at;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}

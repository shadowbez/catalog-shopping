package com.example.catalogshopping.model;

public class Product {
    private String id;
    private String name;
    private double price;
    private String desc;
    private int quantity;

    public Product(String id, String name, double price, String desc, int quantity) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.desc = desc;
        this.quantity = quantity;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public String getDesc() {
        return desc;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}

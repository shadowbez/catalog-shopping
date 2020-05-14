package com.example.catalogshopping.model;

/**
 * POJO FOR RETRIEVING ITEMS FROM FIRESTORE DATABASE
 */
public class ProductFirestore {

    private String id;
    private String description;
    private String name;
    private double price;

    public ProductFirestore() {

    }
    public ProductFirestore(String id, String description, String name, double price) {
        this.id = id;
        this.description = description;
        this.name = name;
        this.price = price;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return "ProductFirestore{" +
                "id='" + id + '\'' +
                ", description='" + description + '\'' +
                ", name='" + name + '\'' +
                ", price=" + price +
                '}';
    }
}

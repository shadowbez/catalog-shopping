package com.example.catalogshopping.model;

import java.util.ArrayList;
import java.util.List;

public class ShoppingCart {
    private double overallPrice;
    private List<Product> products;
//    private List<ProductFirestore> products;

    public ShoppingCart() {
        products = new ArrayList<>();
        overallPrice = 0d;
    }

    public void addAll(List<Product> otherProducts) {
        for (Product elem : otherProducts) {
            addItem(elem);
        }
    }

    public int addItem(Product product) {
        int existPlace = exists(product.getProductFirestore().getId());
        if (existPlace >= 0) {
//            products.get(existPlace).setQuantity(products.get(existPlace).getQuantity() + product.getQuantity());
        } else {
            products.add(product);
        }
//        overallPrice += (product.getPrice() * product.getQuantity());

        return existPlace;
    }

    public int removeItem(String id) {
        int removed = exists(id);

        if (removed >= 0) {
            products.remove(removed);
        }

        return removed;
    }

    public double getOverallPrice() {
        return overallPrice;
    }

    public void setOverallPrice(double overallPrice) {
        this.overallPrice = overallPrice;
    }

    public List<Product> getProducts() {
        return products;
    }

    private int exists(String id) {
        int place = -1;

        for (int i = 0; i < products.size(); i++) {
            if (products.get(i).getProductFirestore().getId().equalsIgnoreCase(id)) {
                place = i;
                break;
            }
        }

        return place;
    }
}

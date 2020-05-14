package com.example.catalogshopping.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Data class for shopping cart functionality.
 */
public class ShoppingCart {

    /**
     * Products stored in a list.
     */
    private List<Product> products;

    public ShoppingCart() {
        products = new ArrayList<>();
    }

    public List<Product> getProducts() {
        return products;
    }

    /**
     * Add all elements from a different list.
     * @param otherProducts the other list
     */
    public void addAll(List<Product> otherProducts) {
        for (Product elem : otherProducts) {
            addItem(elem);
        }
    }

    /**
     * Add one unique item to the cart, if exists then do not add
     * @param product the product to be added
     * @return position of item
     */
    public int addItem(Product product) {
        int existPlace = exists(product.getProductFirestore().getId());
        if (existPlace < 0) {
            products.add(product);
        }

        return existPlace;
    }

    /**
     * Safely remove an item from shopping cart. Will only remove if it is present.
     * @param id the id of the item to be removed
     * @return the position of remove product.
     */
    public int removeItem(String id) {
        int removed = exists(id);

        if (removed >= 0) {
            products.remove(removed);
        }

        return removed;
    }

    /**
     * Helper method which checks if the product already exists.
     * @param id The id of the product to check
     * @return the position of the product. If it does not exist it will be -1.
     */
    public int exists(String id) {
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

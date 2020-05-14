package com.example.catalogshopping.model;

import java.io.File;

/**
 * Data class for storing and using a product locally.
 */
public class Product {

    /**
     * Main data of Product object. Fetched form POJO ProductFirestore
     */
    private ProductFirestore productFirestore;

    /**
     * Reference to file of image.
     */
    private File image;
    /**
     * Reference to file of model.
     */
    private File model;

    public Product(ProductFirestore productFirestore, File image, File model) {
        this.productFirestore = productFirestore;
        this.image = image;
        this.model = model;
    }

    public ProductFirestore getProductFirestore() {
        return productFirestore;
    }

    public File getImage() {
        return image;
    }

    public void setImage(File image) {
        this.image = image;
    }

    public File getModel() {
        return model;
    }

    public void setModel(File model) {
        this.model = model;
    }
}

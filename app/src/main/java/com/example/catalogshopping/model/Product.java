package com.example.catalogshopping.model;

import java.io.File;

public class Product {

    private ProductFirestore productFirestore;
    private File image;
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

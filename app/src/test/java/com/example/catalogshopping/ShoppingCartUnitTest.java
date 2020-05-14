package com.example.catalogshopping;

import com.example.catalogshopping.model.Product;
import com.example.catalogshopping.model.ShoppingCart;
import com.example.catalogshopping.other.Util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for ShoppingCart class' methods
 */
public class ShoppingCartUnitTest {

    /**
     * Object to be tested.
     */
    private ShoppingCart shoppingCart;

    /**
     * Setting up the object to be tested. Executed before all tests.
     */
    @Before
    public void setUp() {
        shoppingCart = new ShoppingCart();
    }

    /**
     * Nullifying the object for next test case. Not necessary to nullify but good practice.
     */
    @After
    public void tearDown() {
        shoppingCart = null;
    }

    /**
     * Test addAll method by adding a whole List of Products.
     */
    @Test
    public void addAllTest() {
        Product p1 = new Product(Util.dummyProductFirestore("100"), null, null);
        Product p2 = new Product(Util.dummyProductFirestore("200"), null, null);
        Product p3 = new Product(Util.dummyProductFirestore("300"), null, null);
        List<Product> otherList = new ArrayList<>();
        otherList.add(p1);
        otherList.add(p2);
        otherList.add(p3);

        shoppingCart.addAll(otherList);

        assertTrue(shoppingCart.getProducts().get(0).getProductFirestore().getId().equalsIgnoreCase("100"));
        assertTrue(shoppingCart.getProducts().get(1).getProductFirestore().getId().equalsIgnoreCase("200"));
        assertTrue(shoppingCart.getProducts().get(2).getProductFirestore().getId().equalsIgnoreCase("300"));
    }

    /**
     * Test addItem method by adding unique elements to the shopping cart.
     */
    @Test
    public void addItemTest_unique() {
        Product p1 = new Product(Util.dummyProductFirestore("100"), null, null);
        Product p2 = new Product(Util.dummyProductFirestore("200"), null, null);

        shoppingCart.addItem(p1);
        shoppingCart.addItem(p2);

        assertEquals(shoppingCart.getProducts().size(), 2);
    }

    /**
     * Test addItem method by adding duplicate elements to the shopping cart.
     */
    @Test
    public void addItemTest_duplicate() {
        Product p1 = new Product(Util.dummyProductFirestore("100"), null, null);

        shoppingCart.addItem(p1);
        shoppingCart.addItem(p1);

        assertEquals(shoppingCart.getProducts().size(), 1);
    }

    /**
     * Test removeItem method.
     */
    @Test
    public void removeItemTest() {
        Product p1 = new Product(Util.dummyProductFirestore("100"), null, null);
        Product p2 = new Product(Util.dummyProductFirestore("200"), null, null);
        shoppingCart.addItem(p1);
        shoppingCart.addItem(p2);

        shoppingCart.removeItem("100");

        assertTrue(shoppingCart.getProducts().get(0).getProductFirestore().getId().equalsIgnoreCase("200"));
    }

    /**
     * Test helper method exists.
     */
    @Test
    public void existsTest() {
        Product p1 = new Product(Util.dummyProductFirestore("100"), null, null);
        Product p2 = new Product(Util.dummyProductFirestore("200"), null, null);
        Product p3 = new Product(Util.dummyProductFirestore("400"), null, null);
        shoppingCart.addItem(p1);
        shoppingCart.addItem(p2);
        shoppingCart.addItem(p3);


        assertEquals(shoppingCart.exists("100"), 0);
        assertEquals(shoppingCart.exists("200"), 1);
        assertEquals(shoppingCart.exists("400"), 2);
    }
}
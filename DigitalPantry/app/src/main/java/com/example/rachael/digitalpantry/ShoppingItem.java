package com.example.rachael.digitalpantry;

/**
 * Created by Rachael on 12/12/2016.
 */

public class ShoppingItem {

    public String name;
    public String imageURL;
    public Double cost;
    public int quantity;

    public ShoppingItem (String name, String imageURL, Double cost, int quantity) {
        this.name = name;
        this.imageURL = imageURL;
        this.quantity = quantity;
        this.cost = cost;
    }
}

package com.example.rachael.digitalpantry;

/**
 * Created by Rachael on 12/12/2016.
 */

public class ListItem {
    public String imageUrl;
    public String name;
    public String id;
    public String quantity;

    public ListItem (String name, String imageUrl) {
        this.imageUrl = imageUrl;
        this.name = name;
    }
}

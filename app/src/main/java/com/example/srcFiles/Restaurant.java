// Restaurant.java
package com.example.srcFiles;
import java.util.List;
import java.util.ArrayList;

public class Restaurant {
    private String name;
    private String location;
    private List<Product> menu;
    private double ratings;
    


    public Restaurant(String name, String location, double ratings) {
        this.name = name;
        this.location = location;
        this.menu = new ArrayList<>();
        this.ratings = ratings;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public List<Product> getMenu() {
        return menu;
    }

    public void setMenu(List<Product> menu) {
        this.menu = menu;
    }

    public double getRatings() {
        return ratings;
    }

    public void setRatings(double ratings) {
        this.ratings = ratings;
    }

    
    public void addProduct(Product product) {
        this.menu.add(product);
    }

    
    public void removeProduct(Product product) {
        this.menu.remove(product);
    }
}

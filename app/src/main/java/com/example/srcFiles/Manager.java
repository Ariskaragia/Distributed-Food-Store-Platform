package com.example.srcFiles;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;

public class Manager implements Serializable {
    private String name;
    private List<Restaurant> restaurants;


    public Manager(String name) {
        this.name = name;
        this.restaurants = new ArrayList<>();

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Restaurant> getRestaurants() {
        return restaurants;
    }

    public void setRestaurants(List<Restaurant> restaurants) {
        this.restaurants = restaurants;
    }

    public void addRestaurant(Restaurant restaurant) {
        this.restaurants.add(restaurant);
    }

    public void removeRestaurant(Restaurant restaurant) {
        this.restaurants.remove(restaurant);
    }

    public void RemoveProductData(Store store,Product product,ObjectOutputStream out) throws IOException {
        out.writeObject("remove_product");
        out.writeObject(store.getStoreName());
        out.writeObject(product.getProductName());
        out.flush();
    }

    public void UpdateStockData(Store store,Product product,String amount,ObjectOutputStream out) throws IOException {
        out.writeObject("update_stock");
        out.writeObject(store.getStoreName());
        out.writeObject(product.getProductName());
        out.writeObject(amount);
        out.flush();
    }

    public void ToggleVisibilityData(Store store,Product product,String visible,ObjectOutputStream out) throws IOException {

        out.writeObject("toggle_visibility");
        out.writeObject(store.getStoreName());
        out.writeObject(product.getProductName());
        out.writeObject(visible);
        out.flush();
    }

    public void AddProductData(String storeName,String productName,String productType,String price,String amount,ObjectOutputStream out) throws IOException {

        out.writeObject("add_product");
        out.writeObject(storeName);
        out.writeObject(productName);
        out.writeObject(productType);
        out.writeObject(price);
        out.writeObject(amount);
        out.flush();
    }

    public void AddStoreData(String jsonPath,String logoPath,ObjectOutputStream out) throws IOException {

        out.writeObject("add_store");
        out.writeObject(jsonPath);
        out.writeObject(logoPath);
        out.flush();

    }

    public List<Store> ListStores(ObjectOutputStream out, ObjectInputStream in) throws IOException, ClassNotFoundException {
        out.writeObject("list_stores");
        out.flush();

        List<Store> response = (List<Store>) in.readObject();
        return response;
    }


}
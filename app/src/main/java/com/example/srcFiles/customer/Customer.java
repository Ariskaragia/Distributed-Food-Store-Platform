package com.example.srcFiles.customer;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.*;

import com.example.StoreCallback;
import com.example.srcFiles.*;
public class Customer implements Serializable {
    private String name;
    private String latitude;
    private String longitude;
    private List<Order> orderHistory;

    
    public Customer(String name, String latitude, String longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.orderHistory = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) { this.longitude = longitude; }

    public List<Order> getOrderHistory() {
        return orderHistory;
    }

    public void setOrderHistory(List<Order> orderHistory) {
        this.orderHistory = orderHistory;
    }

    public void addOrder(Order order) {
        this.orderHistory.add(order);
    }



    public void search(SearchRequest request, ObjectOutputStream out, ObjectInputStream in, StoreCallback callback) throws IOException, ClassNotFoundException {



                out.writeObject("search_store");
                out.writeObject(request);
                out.flush();

                List<Store> response = (List<Store>) in.readObject();
                callback.onSuccess(response);


    }

    public synchronized boolean buy(Store store,
                                    Product product,
                                    int amount,
                                    ObjectOutputStream out,
                                    ObjectInputStream in)
            throws IOException, ClassNotFoundException {

        if (product == null) {
            return false;
        }

        if (product.productPurchase(amount)) {
            out.writeObject("update_stock");
            out.writeObject(store.getStoreName());
            out.writeObject(product.getProductName());
            out.writeObject(String.valueOf(product.getAvailableAmount()));
            out.flush();

            String ack = (String) in.readObject();
            return ack != null && !ack.toLowerCase().startsWith("error");
        } else {
            return false;
        }
    }

    public boolean rating(Store store, int stars, ObjectOutputStream out, ObjectInputStream in) throws IOException {

        out.writeObject("rate_store");
        out.writeObject(store.getStoreName());
        out.writeObject(stars);
        out.flush();

        return in.readBoolean();
    }

}


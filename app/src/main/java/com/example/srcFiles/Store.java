package com.example.srcFiles;

import java.io.Serializable;
import java.util.*;

public class Store implements Serializable {
    private String storeName;
    private double latitude;
    private double longitude;
    private String foodCategory;
    private int stars;
    private int noOfVotes;
    private String storeLogo;
    private String PriceCategory;
    private List<Product> products;

    public Store() {
        this.products = new ArrayList<>();
    }

    public synchronized String getStoreName() {
        return storeName;
    }
    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public synchronized double getLatitude() {
        return latitude;
    }
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public synchronized double getLongitude() {
        return longitude;
    }
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public synchronized String getFoodCategory() {
        return foodCategory;
    }
    public void setFoodCategory(String foodCategory) {
        this.foodCategory = foodCategory;
    }

    public synchronized int getStars() {
        return stars;
    }
    public void setStars(int stars) {
        this.stars = stars;
    }

    public synchronized int getNoOfVotes() {
        return noOfVotes;
    }
    public void setNoOfVotes(int noOfVotes) {
        this.noOfVotes = noOfVotes;
    }

    public synchronized String getPriceCategory() {
        if (PriceCategory == null) {
            calculatePriceCategory();
        }
        return PriceCategory;
    }

    public void setPriceCategory(String priceCategory) {
        this.PriceCategory = priceCategory;
    }

    public synchronized String getStoreLogo() {
        return storeLogo;
    }
    public void setStoreLogo(String storeLogo) {
        this.storeLogo = storeLogo;
    }

    public synchronized List<Product> getProducts() {
        return products;
    }
    public void setProducts(List<Product> products) {
        this.products = products;
        calculatePriceCategory();
    }

     public synchronized List<Product> VisibleProductsInStock(){
        List<Product> list=new ArrayList<>();
        for(Product p : products){
            if(p.isVisible() && p.getAvailableAmount()>0){
                list.add(p);
            }
        }
        return list;
     }

    public synchronized String ProductsToString() {
        StringBuilder s = new StringBuilder();
        int i = 0;
        for (Product p : products) {
            ++i;
            s.append(i).append(". ").append(p.toString()).append("\n");
        }
        return s.toString();
    }

    public synchronized String ProductsInStockToString() {
        StringBuilder s = new StringBuilder();
        int i = 0;
        for (Product p : products) {
            if (p.getAvailableAmount() > 0) {
                ++i;
                s.append(i).append(". ").append(p.toString()).append("\n");
            }
        }
        return s.toString();
    }

    public synchronized void calculatePriceCategory() {
        if (products == null || products.isEmpty()) {
            this.PriceCategory = "$";
            return;
        }
        double sum = 0.0;
        for (Product p : products) {
            sum += p.getPrice();
        }
        double avg = sum / products.size();
        if (avg <= 5) {
            setPriceCategory("$");
        } else if (avg <= 15) {
            setPriceCategory("$$");
        } else {
            setPriceCategory("$$$");
        }
    }

    public synchronized void addReview(int newRating) {
        stars = (stars * noOfVotes + newRating) / (noOfVotes + 1);
        noOfVotes++;
    }

    @Override
    public String toString() {
        return "Name: " + this.storeName + "\n Category: " + this.foodCategory + "\n Stars: " + this.stars +
                "\nNumber of Reviews: " + this.noOfVotes +
                "\nPrice Category: " + this.getPriceCategory() +
                "\nLatitude: " + this.latitude + "\nLongitude: " + this.longitude;
    }
}

package com.example.srcFiles;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SearchRequest implements Serializable {
    private double latitude, longitude;
    private int stars;
    private List<String> FoodCategories,PriceCategories;

    public SearchRequest(double latitude,double longitude){
        this.latitude=latitude;
        this.longitude = longitude;
        this.stars=0;
        this.FoodCategories = new ArrayList<>();
        this.PriceCategories = new ArrayList<>();

    }

    public SearchRequest(double latitude, double longitude, int stars, List<String> FoodCategories, List<String> PriceCategories) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.stars = stars;
        this.FoodCategories = (FoodCategories != null) ? FoodCategories : new ArrayList<>();
        this.PriceCategories = (PriceCategories != null && !PriceCategories.isEmpty()) ? PriceCategories : new ArrayList<>();

        // Default all prices if empty
        if (this.PriceCategories.isEmpty()) {
            this.PriceCategories.add("€");
            this.PriceCategories.add("€€");
            this.PriceCategories.add("€€€");
        }
    }


    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public int getStars() { return stars;}
    public List<String> getFoodCategories() { return FoodCategories; }
    public List<String> getPriceCategories() { return PriceCategories; }

}


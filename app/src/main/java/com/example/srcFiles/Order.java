// Order.java
package com.example.srcFiles;
import java.util.List;
import java.util.ArrayList;
import com.example.srcFiles.customer.Customer;

public class Order {
    private Customer customer;
    private Restaurant restaurant;
    private List<Product> itemsOrdered;
    private double totalPrice;
    private String orderStatus;

    
    public Order(Customer customer, Restaurant restaurant) {
        this.customer = customer;
        this.restaurant = restaurant;
        this.itemsOrdered = new ArrayList<>();
        this.totalPrice = 0;
        this.orderStatus = "Pending"; 
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(Restaurant restaurant) {
        this.restaurant = restaurant;
    }

    public List<Product> getItemsOrdered() {
        return itemsOrdered;
    }

    public void setItemsOrdered(List<Product> itemsOrdered) {
        this.itemsOrdered = itemsOrdered;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public void addItem(Product product) {
        this.itemsOrdered.add(product);
        this.totalPrice += product.getPrice();
    }

    public void removeItem(Product product) {
        this.itemsOrdered.remove(product);
        this.totalPrice -= product.getPrice();
    }
}
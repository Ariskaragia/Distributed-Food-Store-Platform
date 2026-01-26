package com.example.srcFiles;

import java.io.Serializable;

/**
 * Product entity shared between Master, Workers and Clients.
 * <p>
 *   In the original version the field that keeps *how many* items have
 *   been sold (unitsSold) was missing, so getUnitsSold() always returned 0
 *   and sales queries showed a total of 0.  This revision adds the
 *   missing state and updates the core purchase primitive so that every
 *   time stock moves, both {@code availableAmount} and {@code unitsSold}
 *   remain consistent.
 * </p>
 */
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    /* -------------------------------------------------------------- */
    // Basic attributes
    /* -------------------------------------------------------------- */
    private String  productName;
    private String  productType;
    private double  price;            // €
    private int     availableAmount;  // remaining stock
    private boolean flag =true;
    private boolean visible = true; // default true

    /* -------------------------------------------------------------- */
    // NEW state – live sales metrics
    /* -------------------------------------------------------------- */
    private int     unitsSold;        // pieces that have been sold so far

    /* -------------------------------------------------------------- */
    // Constructors
    /* -------------------------------------------------------------- */
    public Product() {
        // no‑arg constructor for (de)serialization libraries
    }

    public Product(String productName,
                   String productType,
                   double price,
                   int    availableAmount) {
        this.productName     = productName;
        this.productType     = productType;
        this.price           = price;
        this.availableAmount = availableAmount;
        this.unitsSold       = 0;
    }

    /* -------------------------------------------------------------- */
    // Core stock movement primitives – always use these
    /* -------------------------------------------------------------- */

    /**
     * Attempts to buy {@code qty} items.  Succeeds only if there is
     * enough stock.  On success, both stock and the «units sold» counter
     * are updated atomically.
     *
     * @param qty how many the customer wants to buy
     * @return    {@code true} if the purchase can proceed
     */
    public synchronized boolean productPurchase(int qty) {
        if (qty <= 0 || qty > availableAmount) return false;
        availableAmount -= qty;
        unitsSold      += qty;
        return true;
    }

    public  boolean isVisible() {
        return visible;
    }
    
    public  void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    /**
     * Adds new items to stock (manager restock).
     */
    public synchronized void restock(int qty) {
        if (qty > 0) availableAmount += qty;
    }

    /* -------------------------------------------------------------- */
    // Getters / Setters (thread‑safe because Product travels between
    // JVMs where we do not share the same «synchronized(this)» locks)
    /* -------------------------------------------------------------- */
    public synchronized String getProductName()   { return productName; }
    public synchronized void   setProductName(String name)  { this.productName = name; }

    public synchronized String getProductType()   { return productType; }
    public synchronized void   setProductType(String type)  { this.productType = type; }

    public synchronized double getPrice()         { return price; }
    public synchronized void   setPrice(double p) { this.price = p; }

    public synchronized int    getAvailableAmount()       { return availableAmount; }
    public synchronized void   setAvailableAmount(int a)  { this.availableAmount = a; }

    /* -------------   SALES METRICS   ------------- */
    public synchronized int getUnitsSold()  { return unitsSold; }

    public synchronized void gettrue(){
        this.flag=true;
    }

    public synchronized void getfalse(){
        this.flag=false;
    }

    /**
     * Legacy alias kept so that existing calls in WorkerServer
     * (getSoldAmount()) do not break.  Delegates to getUnitsSold().
     */
    public synchronized int getSoldAmount() { return getUnitsSold(); }

    /**
     * Explicit increment – used when stock is manually reduced via
     * «update_stock» so that reports stay correct.
     */
    public synchronized void incrementSoldAmount(int qty) {
        if (qty > 0) unitsSold += qty;
    }

    /**
     * Convenience helper: money made from this product so far.
     */
    public synchronized double getRevenue() {
        return unitsSold * price;
    }

    /* -------------------------------------------------------------- */
    @Override
    public synchronized String toString() {
        return "%s %.2f€ (stock=%d, sold=%d)".formatted(productName, price, availableAmount, unitsSold);
    }
}


package com.example.srcFiles;
import static com.example.srcFiles.LocationUtils.haversineDistance;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static com.example.srcFiles.ReducerUtils.pushStoresToReducer;

public class WorkerServer {
    private ServerSocket serverSocket;
    private volatile boolean running = true;
    private final Map<String, Store> storeMap = new HashMap<>();
   

    public WorkerServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("[WorkerServer] Listening on port " + port);
    }

    public void start() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                System.out.println("[WorkerServer] Connection accepted from " + socket.getInetAddress());
                new Thread(() -> handleConnection(socket)).start();
            } catch (IOException e) {
                if (!running) break;
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        running = false;
        try { serverSocket.close(); } catch (IOException e) {}
        System.out.println("WorkerServer stopped.");
    }

    private void handleConnection(Socket socket) {
        ObjectInputStream in = null;
        ObjectOutputStream out = null;

        try {
            // Wrap the stream creation in its own try/catch so we can catch EOF here
            try {
                in  = new ObjectInputStream(socket.getInputStream());
                out = new ObjectOutputStream(socket.getOutputStream());
            } catch (EOFException eof) {
                // This was just a “ping” with no ObjectOutputStream header—ignore and close
                return;
            }

            String command = (String) in.readObject();
            System.out.println("[Worker] Received command: " + command);
            switch (command.toLowerCase()) {
                case "add_store":
                    handleAddStore(in, out);
                    break;
                case "add_product": handleAddProduct(in, out); break;
                case "remove_product": handleRemoveProduct(in, out); break;
                case "update_stock": handleUpdateStock(in, out); break;
                case "query_sales": handleQuerySales(in, out); break;
                case "search_store": handleSearchStores(in,out); break;
                case "rate_store": handleRateStore(in,out); break;
                case "toggle_visibility": handleToggleVisibility(in, out); break;

                
                default:
                    System.out.println("[Worker] Unknown command => " + command);
                    out.writeObject("Unknown command => " + command);
            }
            out.flush();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in  != null) in.close();
                if (out != null) out.close();
                socket.close();
            } catch (IOException ignore) {}
        }
    }

    

    private void handleToggleVisibility(ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
        String storeName = (String) in.readObject();
        String productName = (String) in.readObject();
        boolean visible = Boolean.parseBoolean((String) in.readObject());
    
        synchronized (storeMap) {
            Store store = storeMap.get(storeName);
            if (store == null) {
                out.writeObject("Store not found");
                return;
            }
    
            for (Product p : store.getProducts()) {
                if (p.getProductName().equalsIgnoreCase(productName)) {
                    p.setVisible(visible);
    
                    pushStoresToReducer(storeMap.values());
    
                    out.writeObject("Product visibility updated");
                    return;
                }
            }
    
            out.writeObject("Product not found");
        }
    }
    
    
    private void handleAddStore(ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
        String json = (String) in.readObject();
        String logo = (String) in.readObject();
        Store store = parseStoreJson(json);
        store.setStoreLogo(logo);
        synchronized (storeMap) {
            storeMap.put(store.getStoreName(), store);
        }
        System.out.println("[Worker] Store added: " + store.getStoreName());
        System.out.println(store.toString());
        out.writeObject("Worker: Store " + store.getStoreName() + " added OK.");
        pushStoresToReducer(storeMap.values());

    }

    private void handleAddProduct(ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
        String storeName = (String) in.readObject();
        String productName = (String) in.readObject();
        String productType = (String) in.readObject();
        double price = Double.parseDouble((String) in.readObject());
        int amount = Integer.parseInt((String) in.readObject());
        

        synchronized (storeMap) {
            Store store = storeMap.get(storeName);
            if (store == null) {
                out.writeObject("Worker: Store not found => " + storeName);
                return;
            }
            Product p = new Product("","",0.0,0);
            p.setProductName(productName);
            p.setProductType(productType);
            p.setPrice(price);
            p.setAvailableAmount(amount);
            store.getProducts().add(p);
            store.calculatePriceCategory();
        }
        System.out.println("[Worker] Product added: " + productName + " to store: " + storeName);
        out.writeObject("Worker: Product " + productName + " added to " + storeName);
        pushStoresToReducer(storeMap.values());
    }

    private void handleRemoveProduct(ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
        String storeName = (String) in.readObject();
        String productName = (String) in.readObject();

        synchronized (storeMap) {
            Store store = storeMap.get(storeName);
            if (store == null) {
                out.writeObject("Worker: Store not found => " + storeName);
                return;
            }
            boolean found = false;
            for (Product p : store.getProducts()) {
                if (p.getProductName().equalsIgnoreCase(productName)) {
                    p.setVisible(false);
                    found = true;
                    break;
                }
            }
            if (found) {
                System.out.println("[Worker] Product removed: " + productName);
                out.writeObject("Worker: Product removed => " + productName);
                pushStoresToReducer(storeMap.values());
            } else {
                out.writeObject("Worker: Product not found => " + productName);
            }
        }
    }

    private void handleUpdateStock(ObjectInputStream in, ObjectOutputStream out)
            throws IOException, ClassNotFoundException {

        String storeName   = (String) in.readObject();
        String productName = (String) in.readObject();
        

        synchronized (storeMap) {
            Store store = storeMap.get(storeName);
            if (store == null) {
                out.writeObject("Worker: Store not found => " + storeName);
                return;
            }
            Product product = store.getProducts().stream()
                                   .filter(p -> p.getProductName().equalsIgnoreCase(productName))
                                   .findFirst().orElse(null);
                                   if (product == null) {
                                    out.writeObject("Worker: Product not found => " + productName);
                                } else {
                                    int oldAmount = product.getAvailableAmount();
                                    int newAmount = Integer.parseInt((String) in.readObject());
                                    int diff      = Math.max(0, oldAmount - newAmount);
                                    product.setAvailableAmount(newAmount);
                                    product.incrementSoldAmount(diff);
                                    System.out.println("ok: stock updated, +" + diff + " sold");
                                    System.out.println("[Worker] Stock updated ("+storeName+"."+productName+") "
                                                       + oldAmount+" → "+newAmount+" | +" + diff + " sold");
                                    pushStoresToReducer(storeMap.values());
                                    out.writeObject("success");
                                }
        }
    }


    private void handleQuerySales(ObjectInputStream in, ObjectOutputStream out)
            throws IOException, ClassNotFoundException {

        String queryType = (String) in.readObject();
        String category  = (String) in.readObject();

        Map<String,Integer> results = new HashMap<>();

        synchronized (storeMap) {
            for (Store store : storeMap.values()) {
                int count = 0;
                if ("byFoodCategory".equalsIgnoreCase(queryType) &&
                    store.getFoodCategory().equalsIgnoreCase(category)) {

                    for (Product p : store.getProducts()) count += p.getUnitsSold();

                } else if ("byProductType".equalsIgnoreCase(queryType)) {

                    for (Product p : store.getProducts()) {
                        if (p.getProductType().equalsIgnoreCase(category))
                            count += p.getSoldAmount(); 
                    }
                }
                if (count > 0) results.put(store.getStoreName(), count);
            }
        }

        int total = 0;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String,Integer> e : results.entrySet()) {
            sb.append(e.getKey()).append(": ").append(e.getValue()).append(", ");
            total += e.getValue();
        }
        sb.append("total: ").append(total);

        out.writeObject(sb.toString());
        pushStoresToReducer(storeMap.values());
    }


//    private void handleSearchStores(ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
//
//        SearchRequest request = (SearchRequest) in.readObject();
//
//        System.out.println("Incoming SearchRequest: " + request);
//        System.out.println("Available Stores in Worker:");
//        storeMap.values().forEach(store -> System.out.println(store.getStoreName() + " | Category: " + store.getFoodCategory() + " | Price: " + store.getPriceCategory()));
//
//        List<Store> filtered;
//
//        synchronized (storeMap) {
//            if (storeMap.isEmpty()) {
//                pushStoresToReducer(new ArrayList<Store>());
//                return;
//            }
//
//            filtered = storeMap.values().stream()
//                    .filter(store ->
//
//                            haversineDistance(store.getLatitude(), store.getLongitude(),
//                                    request.getLatitude(), request.getLongitude()) <= 5.0
//                            &&
//
//                            store.getStars() >= request.getStars()
//                            &&
//
//                            (request.getFoodCategories().isEmpty() ||
//                                    request.getFoodCategories().stream()
//                                            .anyMatch(cat -> cat.equalsIgnoreCase(store.getFoodCategory()))
//                            )
//                            &&
//
//                            (request.getPriceCategories().contains("all") ||
//                                    request.getPriceCategories().stream()
//                                            .anyMatch(price -> price.equalsIgnoreCase(store.getPriceCategory()))
//                            )
//                    )
//                    .collect(Collectors.toList());
//        }
//
//        System.out.println("Filtered stores count: " + filtered.size());
//        pushStoresToReducer(filtered);
//
//    }

    private void handleSearchStores(ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
        SearchRequest request = (SearchRequest) in.readObject();

        System.out.println("Incoming SearchRequest: " + request);
        System.out.println("Available Stores in Worker:");
        storeMap.values().forEach(store -> System.out.println(store.getStoreName() + " | Category: " + store.getFoodCategory() + " | Price: " + store.getPriceCategory()));

        List<Store> filtered = new ArrayList<>();

        synchronized (storeMap) {
            for (Store store : storeMap.values()) {
                System.out.println("Check store: " + store.getStoreName() + ", stars: " + store.getStars() + ", request stars: " + request.getStars());

                boolean priceMatch = request.getPriceCategories() == null || request.getPriceCategories().isEmpty() ||
                        request.getPriceCategories().stream()
                                .anyMatch(price -> price.trim().equalsIgnoreCase(store.getPriceCategory().trim()));

                boolean foodMatch = request.getFoodCategories().isEmpty() ||
                        request.getFoodCategories().stream().anyMatch(cat -> cat.equalsIgnoreCase(store.getFoodCategory()));

                boolean starsMatch = store.getStars() >= request.getStars();

                boolean distanceMatch = haversineDistance(store.getLatitude(), store.getLongitude(),
                        request.getLatitude(), request.getLongitude()) <= 5.0;



                boolean pass = distanceMatch && starsMatch && foodMatch && priceMatch;

                if (pass) {
                    filtered.add(store);
                }

                System.out.printf("Store: %s | stars: %d | priceMatch: %b | foodMatch: %b | starsMatch: %b | distanceMatch: %b | pass: %b%n",
                        store.getStoreName(), store.getStars(), priceMatch, foodMatch, starsMatch, distanceMatch, pass);
            }
        }

        System.out.println("Filtered stores count: " + filtered.size());
        System.out.println("Request received at Worker: " + request);

        pushStoresToReducer(filtered);

        // ✅ Επιστροφή σήματος στον Master για να ξέρει ότι τελείωσε ο Worker
        out.writeObject("completed");
        out.flush();
    }



    private void handleRateStore(ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
        String storeName = (String) in.readObject();
        int stars = Integer.parseInt((String) in.readObject());
    
        Store store = null;
        synchronized (storeMap) {
            for (String key : storeMap.keySet()) {
                if (key.equalsIgnoreCase(storeName)) {
                    store = storeMap.get(key);
                    break;
                }
            }
        }
    
        if (store != null) {
            store.addReview(stars);
            out.writeObject("ok");
        } else {
            out.writeObject("Not ok");
        }
        out.flush();
    }
    


    private Store parseStoreJson(String jsonData) {
        Gson gson = new Gson();
        Store store = gson.fromJson(jsonData, Store.class);
        store.calculatePriceCategory();
        return store;
    }
    
    

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java WorkerServer <port>");
            return;
        }
        try {
            int port = Integer.parseInt(args[0]);
            WorkerServer ws = new WorkerServer(port);
            Runtime.getRuntime().addShutdownHook(new Thread(ws::stop));
            ws.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
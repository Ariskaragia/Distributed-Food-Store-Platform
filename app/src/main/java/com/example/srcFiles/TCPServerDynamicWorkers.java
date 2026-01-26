package com.example.srcFiles;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

public class TCPServerDynamicWorkers {

    private static final int INITIAL_CORE_POOL_SIZE = 5;
    private static final int MAX_POOL_SIZE = 10;

    private ServerSocket serverSocket;
    private ThreadPoolExecutor executor;
    private SharedData sharedData;          
    private volatile boolean running = true; 


    private final List<Store> stores = new ArrayList<>();

    public TCPServerDynamicWorkers(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        executor = new ThreadPoolExecutor(
                INITIAL_CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>()
        );
        sharedData = new SharedData();
        System.out.println("Server started on port " + port);
    }

    public void start() {

        Thread commandThread = new Thread(this::handleCommands);
        commandThread.start();


        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket);
                
                executor.submit(new ClientHandler(clientSocket));
            } catch (SocketException e) {
                
                if (serverSocket.isClosed()) {
                    System.out.println("Server socket closed, shutting down.");
                } else {
                    System.err.println("Error accepting connection: " + e.getMessage());
                }
            } catch (IOException e) {
                System.err.println("Error accepting connection: " + e.getMessage());
            }
        }
    }

    public void stop() throws IOException {
        running = false;
        serverSocket.close();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("Server stopped.");
    }


    private void handleCommands() {
        Scanner scanner = new Scanner(System.in);
        while (running) {
            System.out.println("Enter command (show/exit):");
            String command = scanner.nextLine().toLowerCase();
            switch (command) {
                case "show":
                    showStores();
                    break;
                case "exit":
                    try {
                        stop();
                    } catch (IOException e) {
                        System.err.println("Error stopping server: " + e.getMessage());
                    }
                    return;
                default:
                    System.err.println("Invalid command.");
            }
        }
    }

    public synchronized void showStores() {
        if (stores.isEmpty()) {
            System.out.println("No stores have been added yet.");
        } else {
            System.out.println("List of stored Stores:");
            for (Store s : stores) {
                System.out.println(" - " + s.getStoreName());
            }
        }
    }


    public synchronized void addStore(Store store) {
        stores.add(store);
        System.out.println("Store added -> " + store.getStoreName() + " (stores.size()=" + stores.size() + ")");
    }


    public synchronized Store findStoreByName(String storeName) {
        for (Store s : stores) {
            if (s.getStoreName().equalsIgnoreCase(storeName)) {
                return s;
            }
        }
        return null;
    }


    private class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
            ) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    
                    String command = inputLine.trim().toLowerCase();
                    System.out.println("Received command: " + command + " from " + clientSocket);

                    switch (command) {
                        case "add_store":
                            handleAddStore(in, out);
                            break;

                        case "add_product":
                            handleAddProduct(in, out);
                            break;

                        case "remove_product":
                            handleRemoveProduct(in, out);
                            break;

                        case "update_stock":
                            handleUpdateStock(in, out);
                            break;

                        case "query_sales":
                            handleQuerySales(in, out);
                            break;

                        default:
                           
                            synchronized (sharedData) {
                                sharedData.incrementValue();
                                out.println("Unrecognized command: " + command
                                         + ". Current value: " + sharedData.getValue());
                            }
                            break;
                    }
                }
            } catch (IOException e) {
                System.err.println("Client disconnected: " + clientSocket.getRemoteSocketAddress()
                                   + " - " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
            }
        }


        private void handleAddStore(BufferedReader in, PrintWriter out) throws IOException {
            String jsonData = in.readLine();  
            String logoPath = in.readLine();   
            System.out.println("Received JSON store data: " + jsonData);
            System.out.println("Received logo path:       " + logoPath);

            try {
                Store store = parseStoreJson(jsonData);
                store.setStoreLogo(logoPath);

              
                addStore(store);

                out.println("Store added successfully.");
            } catch (Exception e) {
                System.err.println("Error parsing JSON: " + e.getMessage());
                out.println("Error parsing JSON.");
            }
        }


        private void handleAddProduct(BufferedReader in, PrintWriter out) throws IOException {
            String storeName = in.readLine();
            String productName = in.readLine();
            String productType = in.readLine();
            double price = Double.parseDouble(in.readLine());
            int amount = Integer.parseInt(in.readLine());

            Store st = findStoreByName(storeName);
            if (st == null) {
                out.println("Store not found: " + storeName);
                return;
            }
          
            Product p = new Product(productName,productType,price,amount);
            p.setProductName(productName);
            p.setProductType(productType);
            p.setPrice(price);
            p.setAvailableAmount(amount);

           
            synchronized (TCPServerDynamicWorkers.this) {
                st.getProducts().add(p);
            }
            out.println("Product added successfully to store " + storeName);
        }


        private void handleRemoveProduct(BufferedReader in, PrintWriter out) throws IOException {
            String storeName = in.readLine();
            String productName = in.readLine();

            Store st = findStoreByName(storeName);
            if (st == null) {
                out.println("Store not found: " + storeName);
                return;
            }

            synchronized (TCPServerDynamicWorkers.this) {
                Product toRemove = null;
                for (Product prod : st.getProducts()) {
                    if (prod.getProductName().equalsIgnoreCase(productName)) {
                        toRemove = prod;
                        break;
                    }
                }
                if (toRemove != null) {
                    st.getProducts().remove(toRemove);
                    out.println("Product '" + productName + "' removed from store " + storeName);
                } else {
                    out.println("Product not found: " + productName);
                }
            }
        }


        private void handleUpdateStock(BufferedReader in, PrintWriter out) throws IOException {
            String storeName = in.readLine();
            String productName = in.readLine();
            int newAmount = Integer.parseInt(in.readLine());

            Store st = findStoreByName(storeName);
            if (st == null) {
                out.println("Store not found: " + storeName);
                return;
            }

            synchronized (TCPServerDynamicWorkers.this) {
                Product target = null;
                for (Product prod : st.getProducts()) {
                    if (prod.getProductName().equalsIgnoreCase(productName)) {
                        target = prod;
                        break;
                    }
                }
                if (target != null) {
                    target.setAvailableAmount(newAmount);
                    out.println("Stock updated. " + productName + " -> " + newAmount);
                } else {
                    out.println("Product not found: " + productName);
                }
            }
        }

        private void handleQuerySales(BufferedReader in, PrintWriter out) throws IOException {
            String queryType = in.readLine(); 
            String category  = in.readLine(); 

           
            out.println("Query: " + queryType + " for category: " + category + " (Not fully implemented yet)");
        }


        private Store parseStoreJson(String jsonData) {
            Store store = new Store();
            store.setStoreName(extractString(jsonData, "\"StoreName\""));
            String latStr = extractNumber(jsonData, "\"Latitude\"");
            if (latStr != null) {
                store.setLatitude(Double.parseDouble(latStr));
            }
            String lonStr = extractNumber(jsonData, "\"Longitude\"");
            if (lonStr != null) {
                store.setLongitude(Double.parseDouble(lonStr));
            }
            store.setFoodCategory(extractString(jsonData, "\"FoodCategory\""));
            String starsStr = extractNumber(jsonData, "\"Stars\"");
            if (starsStr != null) {
                store.setStars(Integer.parseInt(starsStr));
            }
            String votesStr = extractNumber(jsonData, "\"NoOfVotes\"");
            if (votesStr != null) {
                store.setNoOfVotes(Integer.parseInt(votesStr));
            }

            // Parse products array
            String productsArray = extractArray(jsonData, "\"Products\"");
            if (productsArray != null) {
                List<Product> products = parseProducts(productsArray);
                store.setProducts(products);
            }
            return store;
        }

        private String extractString(String json, String key) {
            String keyPattern = key + ":";
            int startIndex = json.indexOf(keyPattern);
            if (startIndex == -1) {
                return null;
            }
            startIndex = json.indexOf("\"", startIndex + keyPattern.length());
            if (startIndex == -1) {
                return null;
            }
            int endIndex = json.indexOf("\"", startIndex + 1);
            if (endIndex == -1) {
                return null;
            }
            return json.substring(startIndex + 1, endIndex);
        }

        private String extractNumber(String json, String key) {
            String keyPattern = key + ":";
            int startIndex = json.indexOf(keyPattern);
            if (startIndex == -1) {
                return null;
            }
            startIndex += keyPattern.length();
            while (startIndex < json.length() &&
                   (json.charAt(startIndex) == ' ' || json.charAt(startIndex) == '\t')) {
                startIndex++;
            }
            int endIndex = startIndex;
            while (endIndex < json.length()) {
                char c = json.charAt(endIndex);
                if (c == ',' || c == '}' || c == ']' || Character.isWhitespace(c)) {
                    break;
                }
                endIndex++;
            }
            return json.substring(startIndex, endIndex);
        }

        private String extractArray(String json, String arrayKey) {
            String keyPattern = arrayKey + ":";
            int startIndex = json.indexOf(keyPattern);
            if (startIndex == -1) {
                return null;
            }
            startIndex = json.indexOf("[", startIndex + keyPattern.length());
            if (startIndex == -1) {
                return null;
            }
            int bracketCount = 0;
            int endIndex = startIndex;
            while (endIndex < json.length()) {
                char c = json.charAt(endIndex);
                if (c == '[') bracketCount++;
                if (c == ']') bracketCount--;
                endIndex++;
                if (bracketCount == 0) break;
            }
            return json.substring(startIndex, endIndex);
        }

        private List<Product> parseProducts(String productsArray) {
            List<Product> products = new ArrayList<>();
            String trimmed = productsArray.trim();
            if (trimmed.startsWith("[")) {
                trimmed = trimmed.substring(1);
            }
            if (trimmed.endsWith("]")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
            String[] rawObjects = trimmed.split("},");
            for (String rawObj : rawObjects) {
                String objStr = rawObj.trim();
                if (!objStr.endsWith("}")) {
                    objStr += "}";
                }
                Product p = parseSingleProduct(objStr);
                if (p != null) {
                    products.add(p);
                }
            }
            return products;
        }

        private Product parseSingleProduct(String productJson) {
            Product p = new Product();
            p.setProductName(extractString(productJson, "\"ProductName\""));
            p.setProductType(extractString(productJson, "\"ProductType\""));
            String amountStr = extractNumber(productJson, "\"Available Amount\"");
            if (amountStr != null) {
                p.setAvailableAmount(Integer.parseInt(amountStr));
            }
            String priceStr = extractNumber(productJson, "\"Price\"");
            if (priceStr != null) {
                p.setPrice(Double.parseDouble(priceStr));
            }
            return p;
        }
    }

    public static void main(String[] args) {
        try {
            TCPServerDynamicWorkers server = new TCPServerDynamicWorkers(65432);
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    server.stop();
                } catch (IOException e) {
                    System.err.println("Error stopping server: " + e.getMessage());
                }
            }));

            server.start();

        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        }
    }
}

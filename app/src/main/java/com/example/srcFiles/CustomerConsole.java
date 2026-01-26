package com.example.srcFiles;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Collectors;



public class CustomerConsole {

    public Store search(ObjectOutputStream out, ObjectInputStream in, Scanner scanner) throws IOException, ClassNotFoundException {
        System.out.println("Latitude: ");
        double latitude = Double.parseDouble(scanner.nextLine());
        System.out.println("Longitude: ");
        double longitude = Double.parseDouble(scanner.nextLine());
        System.out.println("Stars: ");
        int stars = Integer.parseInt(scanner.nextLine());
        ArrayList<String> fc = new ArrayList<>();
        String answer;
        do {
            System.out.println("Food Category: ");
            String foodCategories = scanner.nextLine().trim();
            fc.add(foodCategories);
            System.out.println("Do you want to add category? (y/n)");
            answer = scanner.nextLine().trim().toLowerCase();

        } while (answer.equals("y"));
        ArrayList<String> pc = new ArrayList<>();
        pc.add("$"); pc.add("$$"); pc.add("$$$");
        do {
            System.out.println("Price category ($,$$,$$$, all): ");
            String priceCategories = scanner.nextLine().trim();
            if(!answer.equals("all")){
                pc.add(priceCategories);
            }
            System.out.println("Do you want to add category? (y/n)");
            answer = scanner.nextLine().trim().toLowerCase();

        } while (answer.equals("y"));
        SearchRequest request = new SearchRequest(latitude, longitude, stars, fc, pc);
        out.writeObject(request);
        out.flush();
       
        List<Store> response = (List<Store>) in.readObject();
        if (response.isEmpty()) {
            System.out.println("No stores fulfill your requirements!");
            return null;
        } else {
            int c = 1;
            for (Store s : response) {
                System.out.println(c + ". " + s.toString() + "\n");
                c++;
            }
            int NoS = 0;
            do {
                System.out.println("Choose a store (type the number):");
                NoS = scanner.nextInt();
            } while (NoS < 1 || NoS > response.size());

            return response.get(NoS - 1);

        }


    }


public synchronized boolean buy(Store store,
                                 Product product,
                                 int amount,
                                 ObjectOutputStream out,
                                 ObjectInputStream in)
        throws IOException, ClassNotFoundException {

    if (product == null) {
        System.out.println("Product no longer available.");
        return false;
    }

    if (product.productPurchase(amount)) {
        out.writeObject("update_stock");
        out.writeObject(store.getStoreName());
        out.writeObject(product.getProductName());
        out.writeObject(String.valueOf(product.getAvailableAmount())); // στέλνω String
        out.flush();


        String ack = (String) in.readObject(); //Διάβασε το confirmation (String) του server
        boolean ok = ack != null && !ack.toLowerCase().startsWith("error");
        System.out.println("[Server]: " + ack);
        return ok;

    } else {
        return false;
    }
}



    boolean rating(ObjectOutputStream out,ObjectInputStream in, Scanner scanner) throws IOException {
        System.out.println("Type the store's name you want to rate:");
        String storeName=scanner.nextLine().trim();
        System.out.println("How many stars do you think this store deserves?");
        int stars = scanner.nextInt();
        scanner.nextLine();        
        out.writeObject(storeName);
        out.writeObject(stars);
        out.flush();

        return in.readBoolean();
    }

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        CustomerConsole customer = new CustomerConsole();
    
        String serverHost = "localhost";
        int serverPort = 65432;
    
        try (
            Socket socket = new Socket(serverHost, serverPort);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            System.out.println("Connected to Master at " + serverHost + ":" + serverPort);
    
            while (true) {
                System.out.println("\nAvailable commands:");
                System.out.println("  search_store  -> Search for a Store");
                System.out.println("  rate_store    -> Rate a Store");
                System.out.println("  exit          -> Exit");
                System.out.print("Enter command: ");
                String command = scanner.nextLine().trim().toLowerCase();
    
                out.writeObject(command);
    
                if ("exit".equals(command)) {
                    System.out.println("Exiting CustomerConsole...");
                    break;
                }
    
                switch (command) {
                    case "search_store":
                        Store chosenStore = customer.search(out, in, scanner);
                        if (chosenStore != null) {
                            String continueShopping = "y";
                            while (continueShopping.equals("y")) {
                                List<Product> products = chosenStore.getProducts().stream()
                                .filter(Product::isVisible)
                                .collect(Collectors.toList());
                                if (products.isEmpty()) {
                                    System.out.println("No products available in this store.");
                                    break;
                                }
    
                                System.out.println("Products:");
                                

                                for (int i = 0; i < products.size(); i++) {
                                    Product p = products.get(i);
                                    System.out.println((i + 1) + ". " + p.getProductName() + " (" + p.getProductType() + ") - " + p.getPrice() + "€ [Available: " + p.getAvailableAmount() + "]");
                                }
    
                                String buyAnswer;
                                do {
                                    System.out.println("Do you want to buy something? (y/n)");
                                    buyAnswer = scanner.nextLine().trim().toLowerCase();
                                } while (!buyAnswer.equals("y") && !buyAnswer.equals("n"));
    
                                if (buyAnswer.equals("y")) {
                                    int productNumber;
                                    do {
                                        System.out.println("Select product number:");
                                        while (!scanner.hasNextInt()) {
                                            System.out.println("Please enter a valid number:");
                                            scanner.next();
                                        }
                                        productNumber = scanner.nextInt();
                                    } while (productNumber < 1 || productNumber > products.size());
    
                                    Product selectedProduct = products.get(productNumber - 1);
    
                                    System.out.println("How many?");
                                    int quantity = scanner.nextInt();
                                    scanner.nextLine();
    
                                    boolean purchased = customer.buy(chosenStore, selectedProduct, quantity, out, in);
                                    if (purchased) {
                                        System.out.println("Purchased successfully!");
                                    } else {
                                        System.out.println("Purchase failed! Not enough stock or invalid amount.");
                                    }
                                }
    
                                System.out.println("Do you want to continue shopping in this store? (y/n)");
                                continueShopping = scanner.nextLine().trim().toLowerCase();
                            }
                        }
                        break;
    
                    case "rate_store":
                        boolean rated = customer.rating(out, in, scanner);
                        if (rated) {
                            System.out.println("Successfully rated the store!");
                        } else {
                            System.out.println("Failed to rate the store.");
                        }
                        break;
    
                    default:
                        System.out.println("Unknown command => " + command);
                }
            }
    
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    




}
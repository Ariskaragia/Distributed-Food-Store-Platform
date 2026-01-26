

package com.example.srcFiles;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Managerconsolee {
    public static void main(String[] args) {
       
            List<WorkerInfo> workers = new ArrayList<>();
       
        String masterHost = "localhost";
        int masterPort =65432 ;
        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            System.out.println("\n=== Manager Console ===");
            System.out.println("1. Add Store");
            System.out.println("2. Add Product");
            System.out.println("3. Remove Product");
            System.out.println("4. Update Stock");
            System.out.println("5. List Stores");
            System.out.println("6. Exit");
            System.out.print("Choice: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    System.out.print("Path to JSON file: ");
                    String jsonPath = scanner.nextLine().trim();
                    System.out.print("Logo path: ");
                    String logoPath = scanner.nextLine().trim();
                    try {
                        String jsonData = new String(Files.readAllBytes(Paths.get(jsonPath)));
                        sendCommand(masterHost, masterPort, out -> {
                            out.println("add_store");
                            out.println(jsonData.replaceAll("\\r?\\n", " "));
                            out.println(logoPath);
                        }, in -> System.out.println(in.readLine()));
                    } catch (IOException e) {
                        System.err.println("Error reading JSON file: " + e.getMessage());
                    }
                    break;
                case "2":
                    System.out.print("Store Name: ");
                    String storeName = scanner.nextLine().trim();
                    System.out.print("Product Name: ");
                    String productName = scanner.nextLine().trim();
                    System.out.print("Product Type: ");
                    String productType = scanner.nextLine().trim();
                    System.out.print("Price: ");
                    String price = scanner.nextLine().trim();
                    System.out.print("Amount: ");
                    String amount = scanner.nextLine().trim();
                    sendCommand(masterHost, masterPort, out -> {
                        out.println("add_product");
                        out.println(storeName);
                        out.println(productName);
                        out.println(productType);
                        out.println(price);
                        out.println(amount);
                    }, in -> System.out.println(in.readLine()));
                    break;
                case "3":
                    System.out.print("Store Name: ");
                    storeName = scanner.nextLine().trim();
                    System.out.print("Product Name: ");
                    productName = scanner.nextLine().trim();
                    sendCommand(masterHost, masterPort, out -> {
                        out.println("remove_product");
                        out.println(storeName);
                        out.println(productName);
                    }, in -> System.out.println(in.readLine()));
                    break;
                case "4":
                    System.out.print("Store Name: ");
                    storeName = scanner.nextLine().trim();
                    System.out.print("Product Name: ");
                    productName = scanner.nextLine().trim();
                    System.out.print("New Amount: ");
                    String newAmount = scanner.nextLine().trim();
                    sendCommand(masterHost, masterPort, out -> {
                        out.println("update_stock");
                        out.println(storeName);
                        out.println(productName);
                        out.println(newAmount);
                    }, in -> System.out.println(in.readLine()));
                    break;
                case "5":
                    sendCommand(masterHost, masterPort, out -> {
                        out.println("show_stores");
                    }, in -> {
                        String line;
                        while ((line = in.readLine()) != null) {
                            System.out.println(line);
                        }
                    });
                    break;
                case "6":
                    exit = true;
                    System.out.println("Exiting Manager Console.");
                    break;
                default:
                    System.out.println("Invalid choice, please try again.");
            }
        }

        scanner.close();
    }

    @FunctionalInterface
    interface OutHandler {
        void handle(PrintWriter out) throws IOException;
    }

    @FunctionalInterface
    interface InHandler {
        void handle(BufferedReader in) throws IOException;
    }

    private static void sendCommand(String host, int port, OutHandler outHandler, InHandler inHandler) {
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            outHandler.handle(out);
            inHandler.handle(in);
        } catch (IOException e) {
            System.err.println("Communication error: " + e.getMessage());
        }
    }
}
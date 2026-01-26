
package com.example.srcFiles;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ManagerConsole {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String serverHost = "localhost";
        int serverPort = 65432;

        try (
            Socket socket = new Socket(serverHost, serverPort);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            System.out.println("Connected to Master at " + serverHost + ":" + serverPort);

            while (true) {
                System.out.println("\n=== Manager Console ===");
                System.out.print("Enter command: ");
                System.out.println("1. add_store");
                System.out.println("2. add_product");
                System.out.println("3. remove_product");
                System.out.println("4. update_stock");
                System.out.println("5. query_sales");
                System.out.println("6. toggle_visibility");
		        System.out.println("7. Exit");
                System.out.print("Choice: ");
                String command = scanner.nextLine().trim().toLowerCase();
                out.writeObject(command);

                if ("exit".equals(command)) {
                    System.out.println("Exiting ManagerConsole...");
                    break;
                }

                switch (command) {
                    case "add_store":
                        sendStoreData(out, scanner);
                        break;
                    case "add_product":
                        sendAddProductData(out, scanner);
                        break;
                    case "remove_product":
                        sendRemoveProductData(out, scanner);
                        break;
                    case "update_stock":
                        sendUpdateStockData(out, scanner);
                        break;
                    case "query_sales":
                        sendQuerySales(out, scanner);
                        break;
                    case "toggle_visibility":
                        sendToggleVisibilityData(out, scanner);
                        break;
                    case "add_worker":
                        sendAddWorkerData(out, scanner);
                        break;
                }

                String response = (String) in.readObject();
                System.out.println("[Master reply]:\n" + response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendAddWorkerData(ObjectOutputStream out, Scanner scanner) throws IOException {
        System.out.print("Worker port: ");
        out.writeObject(scanner.nextLine());
    }

    private static void sendToggleVisibilityData(ObjectOutputStream out, Scanner scanner) throws IOException {
        System.out.print("Store name: "); out.writeObject(scanner.nextLine());
        System.out.print("Product name: "); out.writeObject(scanner.nextLine());
        System.out.print("Visible? (true/false): "); out.writeObject(scanner.nextLine());
    }

    private static void sendStoreData(ObjectOutputStream out, Scanner scanner) throws IOException {
        System.out.print("Enter path to JSON file: ");
        String jsonPath = scanner.nextLine();
        StringBuilder json = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(jsonPath))) {
            String line;
            while ((line = reader.readLine()) != null) json.append(line);
        }

        System.out.print("Enter store logo path: ");
        String logoPath = scanner.nextLine();

        out.writeObject(json.toString());
        out.writeObject(logoPath);
    }

    private static void sendAddProductData(ObjectOutputStream out, Scanner scanner) throws IOException {
        System.out.print("Store name: "); out.writeObject(scanner.nextLine());
        System.out.print("Product name: "); out.writeObject(scanner.nextLine());
        System.out.print("Product type: "); out.writeObject(scanner.nextLine());
        System.out.print("Price: "); out.writeObject(scanner.nextLine());
        System.out.print("Amount: "); out.writeObject(scanner.nextLine());
    }

    private static void sendRemoveProductData(ObjectOutputStream out, Scanner scanner) throws IOException {
        System.out.print("Store name: "); out.writeObject(scanner.nextLine());
        System.out.print("Product name: "); out.writeObject(scanner.nextLine());
    }

    private static void sendUpdateStockData(ObjectOutputStream out, Scanner scanner) throws IOException {
        System.out.print("Store name: "); out.writeObject(scanner.nextLine());
        System.out.print("Product name: "); out.writeObject(scanner.nextLine());
        System.out.print("New amount: "); out.writeObject(scanner.nextLine());
    }

    private static void sendQuerySales(ObjectOutputStream out, Scanner scanner) throws IOException {
        System.out.println("1. byFoodCategory\n2. byProductType");
        String choice = scanner.nextLine().trim();
        String queryType = choice.equals("1") ? "byFoodCategory" : "byProductType";
        System.out.print("Category: ");
        String category = scanner.nextLine().trim();
        out.writeObject(queryType);
        out.writeObject(category);
    }
}

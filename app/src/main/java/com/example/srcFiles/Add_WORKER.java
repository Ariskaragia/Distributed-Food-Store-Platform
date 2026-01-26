package com.example.srcFiles;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Add_WORKER {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String serverHost = "192.168.1.114";
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
                System.out.println("1. add_worker");
                String command = scanner.nextLine().trim().toLowerCase();
                out.writeObject(command);

                if ("exit".equals(command)) {
                    System.out.println("Exiting ManagerConsole...");
                    break;
                }

                switch (command) {
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
        String port =scanner.nextLine();
        System.out.print("Worker host: ");
        String host=scanner.nextLine();
        out.writeObject(port);
        out.writeObject(host);
        ProcessBuilder pb = new ProcessBuilder(
                    "cmd", "/c", "start", "cmd", "/k",
                    "java", "-cp", System.getProperty("java.class.path"),
                    "com.example.WorkerServer", String.valueOf(port));
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.start();
    }

    
}

package com.example.srcFiles;

import java.io.*;
import java.net.*;
import java.util.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class ReducerServer {

    private static final int REDUCER_PORT = 8000;
    private volatile boolean running = true;

    private List<Store> latestSnapshot = new ArrayList<>();
    private int expectedWorkerCount = 0;
    private CountDownLatch latch;

    public static void main(String[] args) {
        new ReducerServer().start();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(REDUCER_PORT)) {
            System.out.println("[Reducer] Server started on port " + REDUCER_PORT);

            while (running) {
                Socket socket = serverSocket.accept();
                new Thread(() -> handleConnection(socket)).start();
            }
        } catch (IOException e) {
            if (running) e.printStackTrace();
        }
    }

    public void stop() {
        running = false;
    }

    private void handleConnection(Socket socket) {
        try (
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        ) {
            String role = (String) in.readObject();

            if ("set_worker_count".equalsIgnoreCase(role)) {
                int count = in.readInt();
                expectedWorkerCount = count;
                latch = new CountDownLatch(expectedWorkerCount);
                latestSnapshot.clear();  // Καθάρισε το παλιό snapshot
                System.out.println("[Reducer] Expected workers set to: " + expectedWorkerCount);
                out.writeObject("ack");
                out.flush();

            } else if ("worker".equalsIgnoreCase(role)) {
                List<Store> pushed = (List<Store>) in.readObject();
                mergeWorkerSnapshot(pushed);
                latch.countDown();  // Ένας worker ολοκλήρωσε

                System.out.println("[Reducer] Received snapshot (size=" + pushed.size() + "). Remaining: " + latch.getCount());
                out.writeObject("ack");
                out.flush();

            } else if ("master".equalsIgnoreCase(role)) {
                if (latch != null) {
                    System.out.println("[Reducer] Waiting for all workers...");
                    latch.await();  // Περιμένει μέχρι να έρθουν όλοι οι workers
                }

                out.writeObject("ack");
                out.flush();

                List<Store> snapshot = pullMergedStores();
                out.writeObject(snapshot);
                out.flush();
                System.out.println("[Reducer] Sent merged list to master (" + snapshot.size() + ")");

            } else {
                System.out.println("[Reducer] Unknown role: " + role);
            }

        } catch (Exception e) {
            System.err.println("[Reducer] Error: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    private void mergeWorkerSnapshot(List<Store> snapshot) {
        synchronized (this) {
            Map<String, Store> map = new HashMap<>();
            for (Store s : snapshot) {
                map.merge(s.getStoreName(), s, (oldStore, newStore) -> {
                    for (Product np : newStore.getProducts()) {
                        oldStore.getProducts().stream()
                                .filter(op -> op.getProductName().equalsIgnoreCase(np.getProductName()))
                                .findFirst()
                                .ifPresentOrElse(
                                        op -> {
                                            // κράτα το μικρότερο διαθέσιμο stock
                                            op.setAvailableAmount(
                                                    Math.min(op.getAvailableAmount(), np.getAvailableAmount()));
                                            // αλλά πρόσθεσε τις πωλήσεις
                                            op.incrementSoldAmount(np.getUnitsSold());
                                        },
                                        () -> oldStore.getProducts().add(np)
                                );
                    }
                    return oldStore;
                });
            }
            latestSnapshot = new ArrayList<>(map.values());
        }
    }


    private List<Store> pullMergedStores() {
        synchronized (this) {
            return new ArrayList<>(latestSnapshot);
        }
    }
}


//public class ReducerServer {
//
//    private static final int REDUCER_PORT = 8000;
//    private volatile boolean running = true;
//
//    // Αντί για snapshots ανά worker, κρατάμε μόνο το τελευταίο snapshot
//    private List<Store> latestSnapshot = new ArrayList<>();
//
//    public static void main(String[] args) {
//        new ReducerServer().start();
//    }
//
//    public void start() {
//        try (ServerSocket serverSocket = new ServerSocket(REDUCER_PORT)) {
//            System.out.println("[Reducer] Server started on port " + REDUCER_PORT);
//
//            while (running) {
//                Socket socket = serverSocket.accept();
//                new Thread(() -> handleConnection(socket)).start();
//            }
//        } catch (IOException e) {
//            if (running) e.printStackTrace();
//        }
//    }
//
//    public void stop() {
//        running = false;
//    }
//
//    private void handleConnection(Socket socket) {
//        try (
//                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
//                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
//        ) {
//            String role = (String) in.readObject();
//
//            if ("worker".equalsIgnoreCase(role)) {
//                List<Store> pushed = (List<Store>) in.readObject();
//                mergeWorkerSnapshot(pushed);
//
//                System.out.println("[Reducer] Snapshot from worker size=" + pushed.size());
//
//                out.writeObject("ack");
//                out.flush();
//
//            } else if ("master".equalsIgnoreCase(role)) {
//                List<Store> snapshot = pullMergedStores();
//
//
//
//                out.writeObject("ack");
//                out.flush();
//
//                out.writeObject(snapshot);
//                out.flush();
//
//                System.out.println("[Reducer] Sent merged list to master (" + snapshot.size() + ")");
//
//            } else {
//                System.out.println("[Reducer] Unknown role: " + role);
//            }
//
//        } catch (Exception e) {
//            System.err.println("[Reducer] error: " + e.getMessage());
//        } finally {
//            try {
//                socket.close();
//            } catch (IOException e) {
//                // ignore
//            }
//        }
//    }
//
//    private void mergeWorkerSnapshot(List<Store> snapshot) {
//        synchronized (this) {
//            latestSnapshot = new ArrayList<>(snapshot);
//            System.out.println("[Reducer] Updated latestSnapshot with size: " + latestSnapshot.size());
//        }
//    }
//
//    private List<Store> pullMergedStores() {
//        synchronized (this) {
//            return new ArrayList<>(latestSnapshot);
//        }
//    }
//}
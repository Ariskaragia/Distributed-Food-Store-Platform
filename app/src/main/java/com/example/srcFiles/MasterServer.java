package com.example.srcFiles;

import com.google.gson.JsonParser;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.util.*;

import static com.example.srcFiles.ReducerUtils.pullStoresFromReducer;

public class MasterServer {
    private final ServerSocket serverSocket;
    private volatile boolean running = true;

    // Λίστα διαθέσιμων Worker κόμβων
    private final List<WorkerInfo> workers = new ArrayList<>();

    // Χαρτογράφηση κάθε storeName σε [primary, replica]
    private final Map<String, List<WorkerInfo>> storeReplicas = new HashMap<>();

    public MasterServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("[Master] Server started on port " + port);
    }

    public void start() {
        // Ξεκινάει thread για τοπική κονσόλα
        new Thread(this::handleLocalCommands, "MasterConsole").start();

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket), "ClientHandler-" + clientSocket.getPort()).start();
            } catch (IOException e) {
                if (!running) break;
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException ignored) {}
        System.out.println("[Master] Server stopped.");
    }

    private void handleLocalCommands() {
        if (System.console() == null) {
            System.out.println("[Master] Console not available. Skipping command input.");
            return;
        }

        Scanner scanner = new Scanner(System.in);
        while (running) {
            System.out.print("Master > ");
            String cmd = scanner.nextLine().trim();
            switch (cmd.toLowerCase()) {
                case "exit":
                    stop();
                    break;
                case "showworkers": {
                    synchronized (workers) {
                        for (int i = 0; i < workers.size(); i++) {
                            WorkerInfo w = workers.get(i);
                            System.out.println("Worker " + i + ": " + w.getHost() + ":" + w.getPort());
                        }
                    }
                } break;
                default:
                    System.out.println("Unknown command: " + cmd);
                    break;
            }
        }
    }

    public void addWorker(String host, int port) {
        synchronized (workers) {
            workers.add(new WorkerInfo(host, port));
        }
    }

    /**
     * Επιλέγει primary & replica για ένα δεδομένο storeName.
     * Αν υπάρχουν N workers, primaryIndex = abs(hash) % N, replicaIndex = (primaryIndex+1)%N.
     */
    private List<WorkerInfo> pickPrimaryAndReplica(String storeName) {
        synchronized (workers) {
            int N = workers.size();
            if (N == 0) throw new IllegalStateException("No workers available");
            int primaryIdx = Math.abs(storeName.hashCode()) % N;
            int replicaIdx = (primaryIdx + 1) % N;
            WorkerInfo primary = workers.get(primaryIdx);
            WorkerInfo replica = workers.get(replicaIdx);
            return Arrays.asList(primary, replica);
        }
    }

    /**
     * Στέλνει μια εντολή (command + args) σε έναν Worker και επιστρέφει την απάντηση.
     * Ρίχνει IOException ή ClassNotFoundException αν κάτι πάει στραβά.
     */
    private String sendToWorker(WorkerInfo w, String command, String... args)
            throws IOException, ClassNotFoundException {
        try (Socket socket = new Socket(w.getHost(), w.getPort());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject(command);
            for (String arg : args) {
                out.writeObject(arg);
            }
            out.flush();
            return (String) in.readObject();
        }
    }

    /**
     * Μέθοδος που κάνει write operation με απλό failover:
     * - Προσπαθεί στον primary.
     * - Αν αποτύχει (IOException/ClassNotFoundException), αφαιρεί τον primary
     *   και προωθεί τον replica σε primary.
     * - Επιστρέφει την απάντηση ή μήνυμα λάθους αν αποτύχει και ο replica.
     */
    private String sendWriteWithFailover(String storeName, String command, String... args) {
        List<WorkerInfo> replicas = storeReplicas.get(storeName);
        if (replicas == null || replicas.isEmpty()) {
            return "ERROR: Store " + storeName + " not found on any worker";
        }
        WorkerInfo primary = replicas.get(0);
        WorkerInfo replica = replicas.size() > 1 ? replicas.get(1) : null;

        // Προσπάθεια στον primary
        try {
            return sendToWorker(primary, command, args);
        } catch (IOException | ClassNotFoundException ePrimary) {
            ePrimary.printStackTrace();
            System.err.println("[Master] Primary failed for store '" +
                    storeName + "': " + primary.getHost() + ":" + primary.getPort());

            // Αφαίρεσε τον primary από τη λίστα workers (θεωρούμε ότι έπεσε)
            synchronized (workers) {
                workers.remove(primary);
            }

            // Αν υπάρχει replica, προώθησέ τον σε primary
            if (replica != null) {
                System.err.println("[Master] Promoting replica to primary for store '" +
                        storeName + "': " + replica.getHost() + ":" + replica.getPort());
                // Ενημέρωση storeReplicas: ο replica τώρα είναι μόνος primary
                storeReplicas.put(storeName, Collections.singletonList(replica));
                try {
                    return sendToWorker(replica, command, args);
                } catch (IOException | ClassNotFoundException e2) {
                    e2.printStackTrace();
                    return "ERROR: Both primary & replica failed for store " + storeName;
                }
            } else {
                return "ERROR: Primary failed & no replica available for store " + storeName;
            }
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (
                    ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                    ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())
            ) {
                while (true) {
                    String command = (String) in.readObject();
                    if (command == null) break;
                    System.out.println("[Master] Received command: " + command);

                    switch (command.toLowerCase()) {
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
                        case "search_store":
                            handleSearchStores(in, out);
                            break;
                        case "rate_store":
                            handleRateStore(in, out);
                            break;
                        case "toggle_visibility":
                            handleToggleVisibility(in, out);
                            break;
                        case "add_worker":
                            handleAddWorker(in, out);
                            break;
                        case "list_stores":
                            handleListStores(out);
                            break;
                        default: {
                            System.out.println("[Master] Unknown command => " + command);
                            out.writeObject("Unknown command => " + command);
                        }
                        break;
                    }
                }
            } catch (EOFException eof) {
                // Client έκλεισε σύνδεση
            } catch (Exception e) {
                System.err.println("[Master] Client error: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException ignored) {
                }
            }
        }

        // ============================
        // === Handlers «read-only» ===
        // ============================

        private void handleListStores(ObjectOutputStream out) throws IOException {
            List<Thread> threads = new ArrayList<>();
            synchronized (workers) {
                for (WorkerInfo w : workers) {
                    Thread t = new Thread(() -> {
                        try {
                            sendToWorker(w, "list_stores");
                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }, "List-Stores-" + w.getPort());
                    t.start();
                    threads.add(t);
                }
            }
            for (Thread t : threads) {
                try {
                    t.join();
                } catch (InterruptedException ignored) {
                }
            }

            List<Store> allStores = pullStoresFromReducer();
            out.writeObject(allStores);
            out.flush();
        }

        private void handleQuerySales(ObjectInputStream in, ObjectOutputStream out)
                throws IOException, ClassNotFoundException {

            String queryType = (String) in.readObject();
            String category = (String) in.readObject();

            List<Store> allStores = pullStoresFromReducer();

            int total = 0;
            StringBuilder sb = new StringBuilder();

            for (Store store : allStores) {
                int count = 0;
                if ("byFoodCategory".equalsIgnoreCase(queryType) &&
                        store.getFoodCategory().equalsIgnoreCase(category)) {

                    for (Product p : store.getProducts()) count += p.getUnitsSold();

                } else if ("byProductType".equalsIgnoreCase(queryType)) {

                    for (Product p : store.getProducts()) {
                        if (p.getProductType().equalsIgnoreCase(category))
                            count += p.getUnitsSold();
                    }
                }
                if (count > 0) {
                    sb.append(store.getStoreName()).append(": ").append(count).append(", ");
                    total += count;
                }
            }
            sb.append("total: ").append(total);
            out.writeObject(sb.toString());
        }

        private void handleSearchStores(ObjectInputStream in, ObjectOutputStream out)
                throws IOException, ClassNotFoundException {

            SearchRequest request = (SearchRequest) in.readObject();

            // 1) Ελέγχουμε ποιοι workers είναι "ζωντανοί"
            List<WorkerInfo> aliveWorkers = new ArrayList<>();
            synchronized (workers) {
                for (WorkerInfo w : new ArrayList<>(workers)) {
                    if (isWorkerAlive(w)) {
                        aliveWorkers.add(w);
                    } else {
                        // Αφαιρούμε τον "πέσμενο" worker από τη λίστα
                        System.err.println("[Master] Worker down: " + w.getHost() + ":" + w.getPort());
                        workers.remove(w);
                    }
                }
            }

            // 2) Ενημερώνουμε τον Reducer πόσοι κόμβοι θα στείλουν αποτελέσματα
            ReducerUtils.setExpectedWorkerCount(aliveWorkers.size());

            // 3) Στέλνουμε το αίτημα μόνο στους ζωντανούς workers
            List<Thread> threads = new ArrayList<>();
            for (WorkerInfo w : aliveWorkers) {
                Thread t = new Thread(() -> sendRequestToWorker(w, request),
                        "Search-Worker-" + w.getPort());
                t.start();
                threads.add(t);
            }

            // 4) Περιμένουμε όλα τα threads να τερματίσουν
            for (Thread t : threads) {
                try {
                    t.join();
                } catch (InterruptedException ignored) {}
            }

            // 5) Τραβάμε τα αποτελέσματα από τον Reducer
            List<Store> merged = pullStoresFromReducer();
            out.writeObject(merged);
            out.flush();
        }

        /**
         * Προσπαθούμε να συνδεθούμε στον worker με σύντομο timeout.
         * Επιστρέφει true αν ο κόμβος ανταποκρίνεται, false σε IOException/ConnectException.
         */
        private boolean isWorkerAlive(WorkerInfo worker) {
            Socket probe = new Socket();
            try {
                SocketAddress addr = new InetSocketAddress(worker.getHost(), worker.getPort());
                probe.connect(addr, 1000);  // wait up to 1 second
                return true;
            } catch (IOException e) {
                return false;
            } finally {
                try {
                    probe.close();
                } catch (IOException ignore) {}
            }
        }

        private void sendRequestToWorker(WorkerInfo worker, SearchRequest request) {
            try (Socket s = new Socket(worker.getHost(), worker.getPort());
                 ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
                out.writeObject("search_store");
                out.writeObject(request);
                out.flush();
                in.readObject(); // αναμονή για ack από τον Worker
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void handleAddWorker(ObjectInputStream in, ObjectOutputStream out)
                throws IOException, ClassNotFoundException {
            int port = Integer.parseInt((String) in.readObject());
            String host = (String) in.readObject();
            addWorker(host, port);
            System.out.println("[Master] Added Worker on port " + port);
        }

        // ============================
        // === Handlers «writes» ======
        // ============================

        private void handleAddStore(ObjectInputStream in, ObjectOutputStream out)
                throws IOException, ClassNotFoundException {
            // Διαβάζουμε JSON + logo
            String json = (String) in.readObject();
            String logo = (String) in.readObject();

            String storeName = JsonParser.parseString(json)
                    .getAsJsonObject()
                    .get("storeName")
                    .getAsString();

            // Επιλογή primary + replica
            List<WorkerInfo> twoWorkers;
            try {
                twoWorkers = pickPrimaryAndReplica(storeName);
            } catch (IllegalStateException e) {
                out.writeObject("ERROR: No workers available to handle store");
                return;
            }
            WorkerInfo primary = twoWorkers.get(0);
            WorkerInfo replica = twoWorkers.get(1);

            // Αποθήκευση στη χαρτογράφηση
            synchronized (storeReplicas) {
                storeReplicas.put(storeName, Arrays.asList(primary, replica));
            }

            // Στέλνουμε "add_store" στον primary
            String primaryResult;
            try {
                primaryResult = sendToWorker(primary, "add_store", json, logo);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                primaryResult = "ERROR: Primary add_store failed on " +
                        primary.getHost() + ":" + primary.getPort();
            }

            // Στέλνουμε και στον replica (failure δεν σταματάει τον πελάτη)
            try {
                sendToWorker(replica, "add_store", json, logo);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                System.err.println("[Master] Replica add_store FAILED on " +
                        replica.getHost() + ":" + replica.getPort());
            }

            // Επιστροφή απάντησης στον client
            out.writeObject(primaryResult);
        }

        private void handleAddProduct(ObjectInputStream in, ObjectOutputStream out)
                throws IOException, ClassNotFoundException {
            String storeName = (String) in.readObject();
            String productName = (String) in.readObject();
            String productType = (String) in.readObject();
            String price = (String) in.readObject();
            String amount = (String) in.readObject();

            // Εκτέλεση write με failover
            String response = sendWriteWithFailover(
                    storeName,
                    "add_product",
                    storeName, productName, productType, price, amount
            );
            out.writeObject(response);
        }

        private void handleRemoveProduct(ObjectInputStream in, ObjectOutputStream out)
                throws IOException, ClassNotFoundException {
            String storeName = (String) in.readObject();
            String productName = (String) in.readObject();

            String response = sendWriteWithFailover(
                    storeName,
                    "remove_product",
                    storeName, productName
            );
            out.writeObject(response);
        }

        private void handleUpdateStock(ObjectInputStream in, ObjectOutputStream out)
                throws IOException, ClassNotFoundException {
            String storeName = (String) in.readObject();
            String productName = (String) in.readObject();
            String amountChange = (String) in.readObject();

            List<WorkerInfo> replicas = storeReplicas.get(storeName);
            if (replicas == null || replicas.isEmpty()) {
                out.writeObject("ERROR: Store " + storeName + " not found on workers");
                return;
            }

            WorkerInfo primary = replicas.get(0);
            WorkerInfo replica = replicas.size() > 1 ? replicas.get(1) : null;

            String command = "update_stock";
            String[] args = {storeName, productName, amountChange};

            try {
                boolean primaryAlive = primary != null && isWorkerAlive(primary);
                boolean replicaAlive = replica != null && isWorkerAlive(replica);

                if (!primaryAlive && !replicaAlive) {
                    out.writeObject("ERROR: No available workers to update stock");
                    return;
                }

                String primaryResponse = "skipped";
                if (primaryAlive) {
                    primaryResponse = sendToWorker(primary, command, args);
                    System.out.println("[Master] Primary response: " + primaryResponse);
                } else {
                    System.err.println("[Master] Primary worker down, skipping");
                }

                String replicaResponse = "skipped";
                if (replicaAlive) {
                    replicaResponse = sendToWorker(replica, command, args);
                    System.out.println("[Master] Replica response: " + replicaResponse);
                } else {
                    System.err.println("[Master] Replica worker down, skipping");
                }

                boolean success = (primaryResponse.equalsIgnoreCase("success") || primaryResponse.equals("skipped")) &&
                        (replicaResponse.equalsIgnoreCase("success") || replicaResponse.equals("skipped"));

                if (success) {
                    out.writeObject("success");
                } else {
                    out.writeObject("ERROR: Inconsistency in stock update");
                }
            } catch (IOException | ClassNotFoundException e) {
                out.writeObject("ERROR: Failed updating stock on workers");
                e.printStackTrace();
            }
        }


        private void handleRateStore(ObjectInputStream in, ObjectOutputStream out)
                throws IOException, ClassNotFoundException {
            String storeName = (String) in.readObject();
            int stars = (Integer) in.readObject();

            List<WorkerInfo> replicas = storeReplicas.get(storeName);
            if (replicas == null || replicas.isEmpty()) {
                out.writeBoolean(false);
                out.flush();
                return;
            }

            WorkerInfo primary = replicas.get(0);
            WorkerInfo replica = replicas.size() > 1 ? replicas.get(1) : null;

            String command = "rate_store";
            String[] args = {storeName, String.valueOf(stars)};

            try {
                boolean primaryAlive = primary != null && isWorkerAlive(primary);
                boolean replicaAlive = replica != null && isWorkerAlive(replica);

                if (!primaryAlive && !replicaAlive) {
                    out.writeBoolean(false);
                    out.flush();
                    return;
                }

                String primaryResponse = "skipped";
                if (primaryAlive) {
                    primaryResponse = sendToWorker(primary, command, args);
                    System.out.println("[Master] Primary rate response: " + primaryResponse);
                } else {
                    System.err.println("[Master] Primary worker down, skipping rate");
                }

                String replicaResponse = "skipped";
                if (replicaAlive) {
                    replicaResponse = sendToWorker(replica, command, args);
                    System.out.println("[Master] Replica rate response: " + replicaResponse);
                } else {
                    System.err.println("[Master] Replica worker down, skipping rate");
                }

                boolean success = (primaryResponse.equalsIgnoreCase("ok") || primaryResponse.equals("skipped")) &&
                        (replicaResponse.equalsIgnoreCase("ok") || replicaResponse.equals("skipped"));

                out.writeBoolean(success);
                out.flush();
            } catch (IOException | ClassNotFoundException e) {
                out.writeBoolean(false);
                out.flush();
                e.printStackTrace();
            }
        }



        private void handleToggleVisibility(ObjectInputStream in, ObjectOutputStream out)
                throws IOException, ClassNotFoundException {
            String storeName = (String) in.readObject();
            String productName = (String) in.readObject();
            boolean visible = Boolean.parseBoolean((String) in.readObject());

            String response = sendWriteWithFailover(
                    storeName,
                    "toggle_visibility",
                    storeName, productName, String.valueOf(visible)
            );
            out.writeObject(response);
        }
    }

    private WorkerInfo selectWorkerByStoreName(String storeName) {
        synchronized (workers) {
            if (workers.isEmpty()) return null;
            int idx = Math.abs(storeName.hashCode()) % workers.size();
            return workers.get(idx);
        }
    }

    public static void main(String[] args) throws IOException {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter number of workers: ");
        int workerCount = Integer.parseInt(sc.nextLine());

        MasterServer master = new MasterServer(65432);

        if (workerCount < 1) {
            System.err.println("At least 1 worker is required.");
            return;
        }

        int basePort = 7000;
        for (int i = 0; i < workerCount; i++) {
            int port = basePort + i;
            ProcessBuilder pb = new ProcessBuilder(
                    "cmd", "/c", "start", "cmd", "/k",
                    "java", "-cp", System.getProperty("java.class.path"),
                    "com.example.WorkerServer", String.valueOf(port)
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[WorkerOutput] " + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            master.addWorker("localhost", port);
            System.out.println("[Master] Started WorkerServer on port " + port);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(master::stop));
        master.start();
    }
}

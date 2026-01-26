package com.example.srcFiles;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ClusterLauncher – ανοίγει ΟΛΟ το cluster από ένα κεντρικό `main` *και* δείχνει τα logs
 *   • Reducer   -> thread (μέσα στο ίδιο JVM)
 *   • Master    -> thread (μέσα στο ίδιο JVM)
 *   • N Workers -> **ξεχωριστό** cmd window ο καθένας (ώστε να βλέπεις live logs).
 *
 *   Εκκίνηση (Windows):
 *       javac  com/example/*.java
 *       java   com.example.ClusterLauncher 3   # 3 workers => 3 παράθυρα cmd
 *
 *   Αν τρέχεις Linux/macOS, άλλαξε το template στην `startWorkerProcess` (π.χ. "x-terminal-emulator -e …").
 */
public class ClusterLauncher {
    private static final int REDUCER_PORT = 8000;
    private static final int MASTER_PORT  = 65432;
    private static final int WORKER_BASE  = 7000;

    private static final List<Process> workerProcs = new ArrayList<>();

    public static void main(String[] args) {
        int numWorkers = args.length > 0 ? parseIntOr(args[0], 2) : 2;

        try {
            // ---------- Reducer (thread) ----------
            ReducerServer reducer = new ReducerServer();
            Thread reducerT = new Thread(reducer::start, "ReducerThread");
            reducerT.start();
            System.out.println("[Launcher] Reducer started on port " + REDUCER_PORT);

            // ---------- Master (thread) ----------
            MasterServer master = new MasterServer(MASTER_PORT);
            Thread masterT = new Thread(master::start, "MasterThread");
            masterT.start();
            System.out.println("[Launcher] Master started on port " + MASTER_PORT);

            // ---------- Workers (new cmd each) ----------
            for (int i = 0; i < numWorkers; i++) {
                int port = WORKER_BASE + i;
                Process p = startWorkerProcess(port);
                workerProcs.add(p);
                master.addWorker("localhost", port);
                System.out.println("[Launcher] Worker launched on port " + port);
            }

            // ---------- Shutdown ----------
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[Launcher] Shutting down …");
                master.stop();
                reducer.stop();
                workerProcs.forEach(Process::destroy);
            }));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* ------------------------------------------------------------------ */
    /** Εκκινεί Worker σε νέο cmd /k ώστε να φαίνονται τα logs. */
    private static Process startWorkerProcess(int port) throws IOException {
        String classPath = System.getProperty("java.class.path");
        ProcessBuilder pb = new ProcessBuilder(
                "cmd", "/c", "start", "cmd", "/k",
                "java", "-cp", classPath,
                "com.example.srcFiles.WorkerServer", String.valueOf(port));
        pb.redirectErrorStream(true); // stdout+stderr στο ίδιο παράθυρο
        return pb.start();
    }

    private static int parseIntOr(String s, int dflt) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return dflt; }
    }
}

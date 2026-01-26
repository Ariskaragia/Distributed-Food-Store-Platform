package com.example.srcFiles;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

public final class ReducerUtils {

    private static final String REDUCER_HOST = "localhost";
    private static final int REDUCER_PORT = 8000;

    public static void setExpectedWorkerCount(int count) {
        try (Socket socket = new Socket(REDUCER_HOST, REDUCER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject("set_worker_count");
            out.writeInt(count);
            out.flush();

            Object ack = in.readObject();
            System.out.println("[ReducerUtils] Reducer acknowledged worker count: " + ack);

        } catch (Exception e) {
            System.err.println("[ReducerUtils] Failed to set expected worker count: " + e.getMessage());
        }
    }

    public static void pushStoresToReducer(Collection<Store> stores) {
        try (Socket s = new Socket(REDUCER_HOST, REDUCER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {

            out.writeObject("worker");
            out.writeObject(stores.stream().collect(Collectors.toList()));
            out.flush();

            Object ack = in.readObject();
            System.out.println("[ReducerUtils] Worker got ack: " + ack);

        } catch (Exception e) {
            System.err.println("[ReducerUtils] push failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Store> pullStoresFromReducer() {
        try (Socket s = new Socket(REDUCER_HOST, REDUCER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {

            out.writeObject("master");
            out.flush();

            Object ack = in.readObject();
            System.out.println("[ReducerUtils] ACK from reducer: " + ack);

            List<Store> snapshot = (List<Store>) in.readObject();
            return snapshot;

        } catch (Exception e) {
            System.err.println("[ReducerUtils] pull failed: " + e.getMessage());
            return List.of();
        }
    }

    private ReducerUtils() {}
}


//public final class ReducerUtils {
//    private static final String REDUCER_HOST = "localhost";
//    private static final int    REDUCER_PORT = 8000;
//
//
//    public static void pushStoresToReducer(Collection<Store> stores) {
//        try (Socket s = new Socket(REDUCER_HOST, REDUCER_PORT);
//             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream())) {
//
//            out.writeObject("worker");      // ρόλος
//            out.writeObject(stores.stream().collect(Collectors.toList())); // List<Store>
//            out.flush();
//
//        } catch (Exception e) {
//            System.err.println("[ReducerUtils] push failed: " + e.getMessage());
//        }
//    }
//
//
//    @SuppressWarnings("unchecked")
//    public static List<Store> pullStoresFromReducer() {
//        try (Socket s = new Socket(REDUCER_HOST, REDUCER_PORT);
//             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
//             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
//
//            out.writeObject("master");
//            out.flush();
//
//            Object ack = in.readObject();
//            System.out.println("[ReducerUtils] ACK: " + ack);
//
//            List<Store> snapshot = (List<Store>) in.readObject();
//            return snapshot;
//
//        } catch (Exception e) {
//            System.err.println("[ReducerUtils] pull failed: " + e.getMessage());
//            return List.of();
//        }
//    }
//
//
//
//    private ReducerUtils() {}
//}

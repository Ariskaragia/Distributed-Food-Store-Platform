package com.example;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ClientConnection {
    private static ClientConnection instance;
    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;

    private final String serverIP = "10.0.2.2";  // IP του Master Server για συσκευή 192.168.1.3 , για emulator 10.0.2.2
    private final int serverPort = 65432;

    private ClientConnection() throws IOException {
        Socket s = new Socket();
        s.connect(new InetSocketAddress(serverIP, serverPort), 10000);
        this.socket = s;
        this.output = new ObjectOutputStream(socket.getOutputStream());
        this.input  = new ObjectInputStream(socket.getInputStream());
    }

    public static synchronized ClientConnection getInstance() throws IOException {
        if (instance == null) {
            instance = new ClientConnection();
        }
        return instance;
    }

    public ObjectOutputStream getOutput() {
        return output;
    }

    public ObjectInputStream getInput() {
        return input;
    }

    public Socket getSocket() {
        return socket;
    }

    public void closeConnection() {
        try {
            if (output != null) output.close();
            if (input != null) input.close();
            if (socket != null && !socket.isClosed()) socket.close();
            instance = null;  // reset for reuse later
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

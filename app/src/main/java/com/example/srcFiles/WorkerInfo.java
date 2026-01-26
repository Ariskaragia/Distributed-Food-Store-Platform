package com.example.srcFiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class WorkerInfo {
    private final String host;
    private final int port;
    public static final List<Integer> MASTER_PORTS = new ArrayList<>(Arrays.asList(7000, 7001, 7002));


    public WorkerInfo(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}

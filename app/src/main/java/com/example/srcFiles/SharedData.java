
package com.example.srcFiles;
public class SharedData {
    private int value = 0;

    public synchronized void incrementValue() {
        value++;
    }

    public synchronized int getValue() {
        return value;
    }
}
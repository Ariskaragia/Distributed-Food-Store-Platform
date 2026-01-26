package com.example;

import com.example.srcFiles.Store;

import java.util.List;

public interface StoreCallback {
    void onSuccess(List<Store> stores);
    void onError(Exception e);
}


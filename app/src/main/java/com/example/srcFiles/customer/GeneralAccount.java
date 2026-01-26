package com.example.srcFiles.customer;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.*;
import android.util.Pair;
import com.example.srcFiles.*;

public class GeneralAccount implements Serializable {
    private String username;
    private String password;
    private Object accountHolder; 


    public GeneralAccount(String username, String password, Object accountHolder) {
        this.username = username;
        this.password = password;
        this.accountHolder = accountHolder;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Object getAccountHolder() {
        return accountHolder;
    }

    public void setAccountHolder(Object accountHolder) {
        this.accountHolder = accountHolder;
    }

    public static Pair<Boolean,GeneralAccount> signIn(String username, String password, HashMap<String,GeneralAccount> users){

        for(GeneralAccount user : users.values()){
            if(user.getUsername().equals(username) && password.equals(user.getPassword())){
                return new Pair<>(true,user);
            }
        }

        return new Pair<>(false,null);
    }

    
    public boolean isManager() {
        return accountHolder instanceof Manager;
    }

    
    public boolean isCustomer() {
        return accountHolder instanceof Customer;
    }


    public boolean register(String username, String password, Object accountHolder, List<GeneralAccount> accounts) {
        
        for (GeneralAccount account : accounts) {
            if (account.getUsername().equals(username)) {
                return false; 
            }
        }
        
        GeneralAccount newAccount = new GeneralAccount(username, password, accountHolder);
        accounts.add(newAccount);
        return true;
    }


}

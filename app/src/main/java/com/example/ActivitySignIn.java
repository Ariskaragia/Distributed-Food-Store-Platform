package com.example;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.srcFiles.customer.Customer;
import com.example.srcFiles.customer.GeneralAccount;
import com.example.srcFiles.Manager;
import com.example.srcFiles.TCPServerDynamicWorkers;

import java.io.IOException;
import java.util.HashMap;

public class ActivitySignIn extends AppCompatActivity {

    private HashMap<String, GeneralAccount> users= new HashMap<>();
    GeneralAccount user;

    private final ActivityResultLauncher<Intent> resultRegisterLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        if (data.hasExtra("account")) {
                            GeneralAccount account = (GeneralAccount) data.getSerializableExtra("account");
                            users.put(account.getUsername(),account);
                        }

                    }
                }

            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        //TEST OBJECTS

        Customer customer= new Customer("elli1","3","2");
        Manager manager=new Manager("elli2");
        GeneralAccount ac=new GeneralAccount("elli1","1234",customer);
        GeneralAccount ac2=new GeneralAccount("elli2","1234",manager);
        users.put("elli1",ac);
        users.put("elli2",ac2);

        //TELOS TEST OBJECTS

        EditText ETusername=findViewById(R.id.username);
        EditText ETpassword= findViewById(R.id.password);
        TextView registerText = findViewById(R.id.registerText);
        Button btnSignIn=findViewById(R.id.btnSignIn);


        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = ETusername.getText().toString().trim();
                String password = ETpassword.getText().toString().trim();

                // Χρήση του UserManager για Sign In
                Pair<Boolean, GeneralAccount> result = GeneralAccount.signIn(username, password, users);

                if (result.first) {
                    user = result.second;
                    Toast.makeText(ActivitySignIn.this, "Welcome "+ user.getUsername(), Toast.LENGTH_SHORT).show();

                    if(user.isManager()) {

                        Intent i = new Intent(ActivitySignIn.this,ActivityManagerHome.class);
                        i.putExtra("user",user);
                        startActivity(i);
                        //resultLauncher.launch(i);
                    }
                    else if (user.isCustomer()){


                        Intent i = new Intent(ActivitySignIn.this,ActivityCustomerHome.class);
                        i.putExtra("user",user);
                        startActivity(i);
                        ////resultLauncher.launch(i);
                    }
                } else {
                    Toast.makeText(ActivitySignIn.this, "Wrong username or password", Toast.LENGTH_SHORT).show();

                }
            }
        });


        registerText.setOnClickListener(v -> {
            Intent i = new Intent(ActivitySignIn.this, ActivityRegisterCustomer.class);
            i.putExtra("users",users);
            resultRegisterLauncher.launch(i);
        });

    }
}
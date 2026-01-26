package com.example;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.srcFiles.customer.Customer;
import com.example.srcFiles.customer.GeneralAccount;

import java.util.HashMap;

public class ActivityRegisterCustomer extends AppCompatActivity {

    boolean existsUsername(HashMap<String,GeneralAccount> users, String newAcUsername){
        for( String u: users.keySet()){
            if(u.equals(newAcUsername)){
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register_customer);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        HashMap<String,GeneralAccount> users= (HashMap<String, GeneralAccount>) getIntent().getSerializableExtra("users");

        EditText ETusername = findViewById(R.id.CRusername);
        EditText ETpassword = findViewById(R.id.CRPassword);
        EditText ETname = findViewById(R.id.CRName);
        EditText ETlatitude = findViewById(R.id.CRLatitude);
        EditText ETlongitude = findViewById(R.id.CRLongitude);
        Button btnSubmit=findViewById(R.id.btnCRsubmit);


        btnSubmit.setOnClickListener(v -> {

            String username=ETusername.getText().toString().trim();
            String password=ETpassword.getText().toString().trim();
            String name=ETname.getText().toString().trim();
            String latitude=ETlatitude.getText().toString().trim();
            String longitude=ETlongitude.getText().toString().trim();


            if(existsUsername(users,username)){
                Toast.makeText(ActivityRegisterCustomer.this, "This username already exists!", Toast.LENGTH_SHORT).show();
            }else{
                if(username.isEmpty() || password.isEmpty() || name.isEmpty() || latitude.isEmpty() || longitude.isEmpty() ){
                    Toast.makeText(ActivityRegisterCustomer.this, "Fulfill all fields!", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(ActivityRegisterCustomer.this, "Created Successfully!", Toast.LENGTH_SHORT).show();
                    Customer customer=new Customer(name,latitude,longitude);
                    GeneralAccount account=new GeneralAccount(username,password,customer);
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("account", account);
                    setResult(RESULT_OK, returnIntent);
                    finish();
                }
            }




        });




    }
}
package com.example;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.srcFiles.customer.GeneralAccount;
import com.example.srcFiles.Manager;
import com.example.srcFiles.Store;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

public class ActivityManagerHome extends AppCompatActivity {

    ClientConnection connection;
    Manager accountHolder;
    StoreAdapter adapter;
    List<Store> allStores;
    String jsonPath, logoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manager_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        GeneralAccount user = (GeneralAccount) getIntent().getSerializableExtra("user");
        accountHolder = (Manager) user.getAccountHolder();

        RecyclerView recyclerView = findViewById(R.id.recyclerViewStoresManager);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        ImageButton btnAddStore = findViewById(R.id.btnAddStore);


        new Thread(() -> {
            try {
                connection = ClientConnection.getInstance();
                ObjectOutputStream out = connection.getOutput();
                ObjectInputStream in = connection.getInput();

                allStores = accountHolder.ListStores(out, in);

                runOnUiThread(() -> {
                    adapter = new StoreAdapter(allStores, store -> {
                        Intent i = new Intent(ActivityManagerHome.this, ActivityDisplayEditRateStore.class);
                        i.putExtra("user", user);
                        i.putExtra("store", store);
                        startActivity(i);
                    });
                    recyclerView.setAdapter(adapter);
                });

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Fail connection of Master Server", Toast.LENGTH_SHORT).show());
            }
        }).start();


        btnAddStore.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_2edittext_input, null);

            EditText ETjson = dialogView.findViewById(R.id.editT1);
            ETjson.setHint("Json path");
            EditText ETlogo = dialogView.findViewById(R.id.editT2);
            ETlogo.setHint("Logo path");

            builder.setView(dialogView)
                    .setTitle("Store Data")
                    .setPositiveButton("Submit", null)
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

            AlertDialog dialog = builder.create();
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);

            dialog.setOnShowListener(dlg -> {
                Button btnSubmit = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                btnSubmit.setOnClickListener(vd -> {
                    String jsonPath = ETjson.getText().toString().trim();
                    String logoPath = ETlogo.getText().toString().trim();

                    if (!jsonPath.isEmpty() && !logoPath.isEmpty()) {
                        new Thread(() -> {
                            try {
                                ObjectOutputStream out = connection.getOutput();
                                accountHolder.AddStoreData(jsonPath, logoPath, out);
                                runOnUiThread(() -> {
                                    Toast.makeText(this, "Store added successfully!", Toast.LENGTH_SHORT).show();
                                    adapter.notifyDataSetChanged();
                                    dialog.dismiss();
                                });
                            } catch (IOException e) {
                                e.printStackTrace();
                                runOnUiThread(() -> Toast.makeText(this, "Something went wrong! Add Store failed", Toast.LENGTH_SHORT).show());
                            }
                        }).start();
                    } else {
                        Toast.makeText(this, "Fulfill all fields!", Toast.LENGTH_SHORT).show();
                    }
                });
            });
            dialog.show();

        });
    }
}

package com.example;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.srcFiles.customer.Customer;
import com.example.srcFiles.customer.GeneralAccount;
import com.example.srcFiles.SearchRequest;
import com.example.srcFiles.Store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ActivityCustomerHome extends AppCompatActivity implements FilterDialogFragment.FilterListener {

    private ClientConnection connection;
    private StoreCallback callback;
    private String latitude;
    private String longitude;
    private List<Store> filteredStores = new ArrayList<>();

    private List<Store> allStores = new ArrayList<>();
    private StoreAdapter adapter;
    private Customer accountHolder;
    private GeneralAccount user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_customer_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        user = (GeneralAccount) getIntent().getSerializableExtra("user");
        accountHolder = (Customer) user.getAccountHolder();
        latitude = accountHolder.getLatitude();
        longitude = accountHolder.getLongitude();

        Button btnLocation = findViewById(R.id.btnChangeLocation);
        ImageButton btnFilter = findViewById(R.id.btnFilter);
        RecyclerView recyclerView = findViewById(R.id.recyclerViewStores);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new StoreAdapter(filteredStores, store -> {
            Intent i = new Intent(ActivityCustomerHome.this, ActivityDisplayEditRateStore.class);
            i.putExtra("user", user);
            i.putExtra("store", store);
            startActivity(i);
        });
        recyclerView.setAdapter(adapter);

        btnLocation.setOnClickListener(v -> showLocationDialog());

        btnFilter.setOnClickListener(v -> {
            if (latitude != null && longitude != null) {

                FilterDialogFragment.newInstance(
                        Double.parseDouble(latitude),
                        Double.parseDouble(longitude),
                        allStores
                ).show(getSupportFragmentManager(), "FilterDialog");
                /*clearResults();*/
            } else {
                Toast.makeText(this, "Enter location first", Toast.LENGTH_SHORT).show();
            }
        });

        callback = new StoreCallback() {
            @Override
            public void onSuccess(List<Store> stores) {
                Log.d("FILTER_RESULT", "Stores returned: " + stores.size());
                runOnUiThread(() -> {

                    for (Store store : stores) {
                        // Παράδειγμα: αν το backend επιστρέφει ένα όνομα εικόνας π.χ. "barbecue"
                        // μπορούμε να κάνουμε:
                        int resId = getResources().getIdentifier(
                                store.getStoreLogo(), // π.χ. "barbecue"
                                "drawable",
                                getPackageName()
                        );

                        if (resId != 0) {
                            // Παίρνουμε το όνομα του resource από το ID
                            String resName = getResources().getResourceEntryName(resId);
                            Log.d("StoreAdapter", "Όνομα drawable: " + resName);
                            // resName θα είναι π.χ. "barbecue"
                        } else {
                            Log.e("StoreAdapter", "Δεν βρέθηκε το drawable με όνομα: " + store.getStoreLogo());
                        }}
                    adapter.updateStoreList(stores);
                    allStores.addAll(stores);


                    filteredStores.clear();
                    filteredStores.addAll(stores);

                    // 3. Ενημέρωση του adapter
                    adapter.updateStoreList(filteredStores);
                    Log.d("STORE_DEBUG", "Search returned: " + stores.size() + " stores.");
                    Toast.makeText(ActivityCustomerHome.this, "Found " + stores.size() + " stores", Toast.LENGTH_SHORT).show();
                    if (stores.isEmpty()) {
                        Toast.makeText(ActivityCustomerHome.this, "No stores found", Toast.LENGTH_SHORT).show();
                    }
                    adapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> Toast.makeText(ActivityCustomerHome.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                reconnectAndRetry();
            }
        };

        // Όταν η σύνδεση γίνει, τρέχει αυτόματα το αρχικό search με latitude & longitude
        new Thread(() -> {
            try {
                connection = ClientConnection.getInstance();
                runOnUiThread(() -> {
                    Toast.makeText(ActivityCustomerHome.this, "Connected", Toast.LENGTH_SHORT).show();
                    if (latitude != null && longitude != null) {
                        performSearch(latitude, longitude);
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(ActivityCustomerHome.this, "Connection failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                e.printStackTrace();
            }
        }).start();
    }

    private void showLocationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_2edittext_input, null);
        EditText ETLat = dialogView.findViewById(R.id.editT1);
        ETLat.setHint("Latitude");
        EditText ETLong = dialogView.findViewById(R.id.editT2);
        ETLong.setHint("Longitude");

        builder.setView(dialogView)
                .setTitle("Location")
                .setPositiveButton("Submit", null)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss());

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String lat = ETLat.getText().toString().trim();
            String lon = ETLong.getText().toString().trim();
            if (!lat.isEmpty() && !lon.isEmpty()) {
                latitude = lat;
                longitude = lon;
                Toast.makeText(this, "Lat: " + latitude + ", Lon: " + longitude, Toast.LENGTH_SHORT).show();
                /*clearResults();*/
                dialog.dismiss();
                // Κάνει καινούριο search με τη νέα τοποθεσία
                if (connection != null) {
                    performSearch(latitude, longitude);
                }
            } else {
                Toast.makeText(this, "Fulfill all fields.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performSearch(String latitude, String longitude) {
        SearchRequest req = new SearchRequest(
                Double.parseDouble(latitude),
                Double.parseDouble(longitude)
        );
        new Thread(() -> {
            try {
                accountHolder.search(req, connection.getOutput(), connection.getInput(), callback);
            } catch (IOException | ClassNotFoundException e) {
                handleSearchError(e);
            }
        }).start();
    }

    @Override
    public void onFiltersSelected(SearchRequest request) {
        performFilteredSearch(request);
    }

    private void performFilteredSearch(SearchRequest request) {
        new Thread(() -> {
            try {
                accountHolder.search(request, connection.getOutput(), connection.getInput(), callback);
            } catch (IOException | ClassNotFoundException e) {
                handleSearchError(e);
            }
        }).start();
    }

    private void handleSearchError(Exception e) {
        Log.e("SEARCH_ERROR", "Error during search", e);
        runOnUiThread(() -> callback.onError(e));
    }

    private void clearResults() {
        runOnUiThread(() -> {
            filteredStores.clear();
            adapter.updateStoreList(filteredStores);
            adapter.notifyDataSetChanged();
        });
    }

    private void reconnectAndRetry() {
        new Thread(() -> {
            try {
                connection = ClientConnection.getInstance();
                runOnUiThread(() -> Toast.makeText(ActivityCustomerHome.this, "Reconnected, retrying search...", Toast.LENGTH_SHORT).show());
                SearchRequest retryReq = new SearchRequest(
                        Double.parseDouble(latitude),
                        Double.parseDouble(longitude)
                );
                accountHolder.search(retryReq, connection.getOutput(), connection.getInput(), callback);
            } catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
                runOnUiThread(() -> Toast.makeText(ActivityCustomerHome.this, "Retry failed: " + ex.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}

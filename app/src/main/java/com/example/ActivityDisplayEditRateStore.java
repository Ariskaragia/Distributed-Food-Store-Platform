package com.example;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import com.example.srcFiles.Store;
import com.example.srcFiles.Manager;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ActivityDisplayEditRateStore extends AppCompatActivity {

    ClientConnection connection;
    GeneralAccount user;

    public boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void showAddProductDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Add New Product");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 30, 40, 30);

        // Δημιουργία EditTexts
        EditText storeNameInput   = createEditText(context, "Store Name");
        EditText productNameInput = createEditText(context, "Product Name");
        EditText productTypeInput = createEditText(context, "Product Type");
        EditText priceInput       = createEditText(context, "Price");
        EditText amountInput      = createEditText(context, "Amount");

        layout.addView(storeNameInput);
        layout.addView(productNameInput);
        layout.addView(productTypeInput);
        layout.addView(priceInput);
        layout.addView(amountInput);

        builder.setView(layout);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String storeName   = storeNameInput.getText().toString().trim();
            String productName = productNameInput.getText().toString().trim();
            String productType = productTypeInput.getText().toString().trim();
            String price       = priceInput.getText().toString().trim();
            String amount      = amountInput.getText().toString().trim();

            try {
                ( (Manager) user.getAccountHolder()).AddProductData(storeName, productName, productType, price, amount,connection.getOutput());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Toast.makeText(context, "Product added: " + productName, Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }


    private EditText createEditText(Context context, String hint) {
        EditText editText = new EditText(context);
        editText.setHint(hint);
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        editText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        return editText;
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_display_edit_rate_store);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        user = (GeneralAccount) getIntent().getSerializableExtra("user");
        Store store= (Store) getIntent().getSerializableExtra("store");
        try {
            connection= ClientConnection.getInstance();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ObjectOutputStream out= connection.getOutput();
        ObjectInputStream in= connection.getInput();

        Button rate=findViewById(R.id.btnRate);
        if(user.isManager()){
            rate.setVisibility(View.INVISIBLE);
        }
        ImageButton addProduct=findViewById(R.id.btnAddProduct);
        if(user.isCustomer()){
            addProduct.setVisibility(View.INVISIBLE);
        }

        TextView tvStoreName= findViewById(R.id.StoreName);
        tvStoreName.setText(store.getStoreName());
        TextView tvFoodCategory= findViewById(R.id.FoodCategory);
        tvFoodCategory.setText("Food Category: "+ store.getFoodCategory());
        TextView tvlatitude= findViewById(R.id.latitude);
        tvlatitude.setText("Latitude: "+ store.getLatitude());
        TextView tvlongitude= findViewById(R.id.longitude);
        tvlongitude.setText("Longitude: "+ store.getLongitude());
        TextView tvstars= findViewById(R.id.stars);
        tvstars.setText("Stars: "+ store.getStars());
        TextView tvNumOfVotes= findViewById(R.id.numOfVotes);
        tvNumOfVotes.setText("No of Votes: "+ store.getNoOfVotes());
        TextView tvPriceCategory= findViewById(R.id.priceCategory);
        tvPriceCategory.setText("Price Category: "+ store.getPriceCategory());

        RecyclerView recyclerView = findViewById(R.id.recyclerViewProducts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ProductAdapter adapter = new ProductAdapter(store.VisibleProductsInStock(),user, product -> {
            if(user.isManager()){
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Επιλογές για: " + product.getProductName());

                String[] options = {"Remove Product", "Hide from Customers", "Update Stock"};

                builder.setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            try {
                                ((Manager) user.getAccountHolder()).RemoveProductData(store,product,out);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            break;
                        case 1:
                            AlertDialog.Builder amountDialog = new AlertDialog.Builder(this);
                            amountDialog.setTitle("Visibility:");

                            EditText amount = new EditText(this);
                            amount.setInputType(InputType.TYPE_CLASS_NUMBER);
                            amount.setHint("true/false");
                            amount.setPadding(40, 30, 40, 30);
                            amountDialog.setView(amount);

                            amountDialog.setPositiveButton("OK", (d, w) -> {
                                String val = amount.getText().toString().trim();
                                if (val.equals("true") || val.equals("false")) {
                                    try {
                                        ((Manager) user.getAccountHolder()).ToggleVisibilityData(store,product,val,out);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                    Toast.makeText(this, "Visibility: " + val, Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
                                }
                            });

                            amountDialog.setNegativeButton("Decline", (d, w) -> d.dismiss());
                            amountDialog.show();
                            break;
                        case 2:
                            AlertDialog.Builder stockDialog = new AlertDialog.Builder(this);
                            stockDialog.setTitle("New Quantity:");

                            EditText input = new EditText(this);
                            input.setInputType(InputType.TYPE_CLASS_NUMBER);
                            input.setPadding(40, 30, 40, 30);
                            stockDialog.setView(input);

                            stockDialog.setPositiveButton("OK", (d, w) -> {
                                String val = input.getText().toString().trim();
                                if (!val.isEmpty() || !isInteger(val) ) {
                                    try {
                                        ((Manager) user.getAccountHolder()).UpdateStockData(store,product,val,out);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                    Toast.makeText(this, "New Quantity: " + val, Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "No changes applied", Toast.LENGTH_SHORT).show();
                                }
                            });

                            stockDialog.setNegativeButton("Decline", (d, w) -> d.dismiss());
                            stockDialog.show();
                            break;
                    }
                });

                builder.setNegativeButton("Decline", (dialog, which) -> dialog.dismiss());
                builder.show();


            }else{ //CUSTOMER
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Purchase: " + product.getProductName());


                final EditText input = new EditText(this);
                input.setHint("Quantity");
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                input.setPadding(50, 40, 50, 40);
                builder.setView(input);


                builder.setPositiveButton("Buy", (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    if (!value.isEmpty()) {
                        int amount = Integer.parseInt(value);

                        new Thread(() -> {
                            boolean success;
                            try {
                                success = ((Customer) user.getAccountHolder()).buy(store, product, amount, out, in);
                                runOnUiThread(() -> {
                                    if (success) {
                                        Toast.makeText(ActivityDisplayEditRateStore.this, "Successfully Purchased!", Toast.LENGTH_SHORT).show();

                                        // ✅ Μετάβαση στην αρχική δραστηριότητα
                                        Intent intent = new Intent(ActivityDisplayEditRateStore.this, ActivityCustomerHome.class);
                                        intent.putExtra("user", user);  // προαιρετικά, αν χρειάζεται
                                        startActivity(intent);
                                        finish(); // ώστε να μη μείνει αυτή η οθόνη στο stack

                                    } else {
                                        Toast.makeText(ActivityDisplayEditRateStore.this, "Purchase Failed :(", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } catch (IOException | ClassNotFoundException e) {
                                e.printStackTrace();
                                runOnUiThread(() ->
                                        Toast.makeText(ActivityDisplayEditRateStore.this, "Error during purchase: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                );
                            }
                        }).start();

                    }
                });

                builder.setNegativeButton("Decline", (dialog, which) -> dialog.cancel());

                builder.show();

            }
        });

        recyclerView.setAdapter(adapter);


        rate.setOnClickListener(v -> {
            AlertDialog.Builder stockDialog = new AlertDialog.Builder(ActivityDisplayEditRateStore.this);
            stockDialog.setTitle("How many stars " + store.getStoreName() + " do you think deserves?");

            EditText input = new EditText(ActivityDisplayEditRateStore.this);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            input.setPadding(40, 30, 40, 30);
            stockDialog.setView(input);

            stockDialog.setPositiveButton("OK", (d, w) -> {
                String val = input.getText().toString().trim();

                if (!val.isEmpty() && isInteger(val)) {
                    int stars = Integer.parseInt(val);

                    if (stars >= 1 && stars <= 5) {
                        new Thread(() -> {
                            boolean success = false;
                            try {
                                success = ((Customer) user.getAccountHolder()).rating(store, stars, out, in);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            boolean finalSuccess = success;
                            runOnUiThread(() -> {
                                if (finalSuccess) {
                                    Toast.makeText(ActivityDisplayEditRateStore.this, "Successfully rated!", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(ActivityDisplayEditRateStore.this, ActivityCustomerHome.class);
                                    intent.putExtra("user", user);  // προαιρετικά, αν χρειάζεται
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(ActivityDisplayEditRateStore.this, "Rating Failure!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }).start();
                    } else {
                        Toast.makeText(this, "Please enter a number between 1 and 5", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Invalid input!", Toast.LENGTH_SHORT).show();
                }
            });

            stockDialog.setNegativeButton("Decline", (d, w) -> d.dismiss());
            stockDialog.show();
        });


        addProduct.setOnClickListener(v -> showAddProductDialog(ActivityDisplayEditRateStore.this));


    }
}
package com.example;


import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.srcFiles.Store;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.StoreViewHolder> {

    public interface OnStoreClickListener {
        void onStoreClick(Store store);
    }

    private List<Store> storeList;
    private OnStoreClickListener listener;

    public StoreAdapter(List<Store> storeList, OnStoreClickListener listener) {
        this.storeList = new ArrayList<>(storeList); // Κάνε copy για ασφάλεια
        this.listener = listener;
    }

    @NonNull
    @Override
    public StoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.store_item, parent, false);
        return new StoreViewHolder(view);
    }

    @Override

    public void onBindViewHolder(@NonNull StoreViewHolder holder, int position) {
        Store store = storeList.get(position);

        // 1. Βάζουμε τα κείμενα στα TextViews
        holder.nameText.setText(store.getStoreName());
        holder.categoryText.setText("Category: " + store.getFoodCategory());
        holder.starsText.setText("Stars: " + store.getStars());
        holder.priceText.setText("Price: " + store.getPriceCategory());

        // 2. “Καθαρίζουμε” το όνομα ώστε να ταιριάξει με το resource filename
        //    π.χ. StoreName "Barbecue Place" → drawableName = "barbecue_place"
        String rawName = store.getStoreName();
        String drawableName = rawName
                .trim()
                .toLowerCase()
                .replace(" ", "_")
                .replaceAll("[^a-z0-9_]", "");
        // Με αυτό αφαιρούμε τυχόν ειδικούς χαρακτήρες, ώστε να είναι έγκυρο ως resource name.

        Context ctx = holder.itemView.getContext();
        int resId = ctx.getResources().getIdentifier(
                drawableName,
                "drawable",
                ctx.getPackageName()
        );

        // 3. Αν βρέθηκε το drawable, το φορτώνουμε. Αλλιώς βάζουμε default.
        if (resId != 0) {
            Glide.with(ctx)
                    .load(resId)
                    .placeholder(R.drawable.placeholder_image)   // όποιο placeholder έχεις
                    .error(R.drawable.default_store_logo)         // fallback αν αποτύχει
                    .centerCrop()
                    .into(holder.logoImage);
        } else {
            holder.logoImage.setImageResource(R.drawable.default_store_logo);
        }

        // 4. OnClickListener
        holder.itemView.setOnClickListener(v -> listener.onStoreClick(store));
    }









    @Override
    public int getItemCount() {
        return storeList.size();
    }

    public void updateStoreList(List<Store> newList) {
        this.storeList = new ArrayList<>(newList);  // φτιάξε ΝΕΑ λίστα, όχι reference


        notifyDataSetChanged();
    }


    static class StoreViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, categoryText, starsText, priceText;
        ImageView logoImage;

        public StoreViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.tvStoreName);
            categoryText = itemView.findViewById(R.id.tvCategory);
            starsText = itemView.findViewById(R.id.tvStars);
            priceText = itemView.findViewById(R.id.tvPriceCategory);
            logoImage = itemView.findViewById(R.id.StoreLogo);
        }

        public void bind(Store store, OnStoreClickListener listener) {
            nameText.setText(store.getStoreName());
            categoryText.setText("Category: " + store.getFoodCategory());
            starsText.setText("Stars: " + store.getStars());
            priceText.setText("Price: " + store.getPriceCategory());


            itemView.setOnClickListener(v -> listener.onStoreClick(store));
        }
    }
}

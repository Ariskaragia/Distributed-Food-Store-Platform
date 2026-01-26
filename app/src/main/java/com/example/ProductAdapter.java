package com.example;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.srcFiles.customer.GeneralAccount;
import com.example.srcFiles.Product;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    private List<Product> productList;
    private static GeneralAccount user;
    private final OnProductClickListener listener;

    public ProductAdapter(List<Product> productList, GeneralAccount user, OnProductClickListener listener) {
        this.productList = productList;
        this.listener = listener;
        this.user=user;
    }

    public void updateProductList(List<Product> newList) {
        this.productList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.product_item, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.bind(product, listener);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName, tvPrice, tvStock, tvSold;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName  = itemView.findViewById(R.id.tvProductName);
            tvPrice = itemView.findViewById(R.id.tvProductPrice);
            tvStock = itemView.findViewById(R.id.tvProductStock);
            tvSold  = itemView.findViewById(R.id.tvProductSold);
        }

        public void bind(Product product, OnProductClickListener listener) {
            tvName.setText(product.getProductName());
            tvPrice.setText(String.format("Price: %.2f€", product.getPrice()));
            tvStock.setText("Stock: " + product.getAvailableAmount());
            if(user.isCustomer()){
                tvSold.setVisibility(View.INVISIBLE);
            }else{
                tvSold.setText("Sold: " + product.getUnitsSold());
            }


            itemView.setOnClickListener(v -> listener.onProductClick(product));
        }
    }
}


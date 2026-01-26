package com.example;

import android.content.Context;
import android.os.Bundle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.srcFiles.SearchRequest;
import com.example.srcFiles.Store;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FilterDialogFragment extends DialogFragment {

    public interface FilterListener {
        void onFiltersSelected(SearchRequest request) throws IOException, ClassNotFoundException;
    }

    private FilterListener listener;
    private double latitude;
    private double longitude;
    private ArrayList<Store> storeList = new ArrayList<>();

    private final Map<CheckBox, String> typeCheckBoxMap = new HashMap<>();
    private CheckBox checkPrice1, checkPrice2, checkPrice3;
    private CheckBox checkStar1, checkStar2, checkStar3, checkStar4, checkStar5;

    public static FilterDialogFragment newInstance(double lat, double lon, List<Store> stores) {
        FilterDialogFragment fragment = new FilterDialogFragment();
        Bundle args = new Bundle();
        args.putDouble("lat", lat);
        args.putDouble("lon", lon);
        args.putSerializable("stores", (Serializable) stores);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof FilterListener) {
            listener = (FilterListener) context;
        } else {
            throw new RuntimeException(context + " must implement FilterListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            latitude = getArguments().getDouble("lat");
            longitude = getArguments().getDouble("lon");
            storeList = (ArrayList<Store>) getArguments().getSerializable("stores");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.filter_dialog_fragment, container, false);

        // Get static checkboxes
        checkPrice1 = view.findViewById(R.id.checkPrice1);
        checkPrice2 = view.findViewById(R.id.checkPrice2);
        checkPrice3 = view.findViewById(R.id.checkPrice3);

        checkStar1 = view.findViewById(R.id.checkStar1);
        checkStar2 = view.findViewById(R.id.checkStar2);
        checkStar3 = view.findViewById(R.id.checkStar3);
        checkStar4 = view.findViewById(R.id.checkStar4);
        checkStar5 = view.findViewById(R.id.checkStar5);

        // Dynamic store types
        LinearLayout containerLayout = view.findViewById(R.id.storeTypeContainer);
        Set<String> types = new HashSet<>();
        for (Store s : storeList) {
            if (s.getFoodCategory() != null) {
                types.add(s.getFoodCategory());
            }
        }

        for (String type : types) {
            CheckBox cb = new CheckBox(getContext());
            cb.setText(type);
            cb.setTextSize(14);
            cb.setPadding(10, 5, 10, 5);
            containerLayout.addView(cb);
            typeCheckBoxMap.put(cb, type);
        }

        Button applyBtn = view.findViewById(R.id.btnApply);
        applyBtn.setOnClickListener(v -> {
            List<String> prices = new ArrayList<>();
            List<String> typesSelected = new ArrayList<>();
            int stars = 0;

            if (checkPrice1.isChecked()) prices.add("$");
            if (checkPrice2.isChecked()) prices.add("$$");
            if (checkPrice3.isChecked()) prices.add("$$$");

            if (checkStar5.isChecked()) stars = 5;
            else if (checkStar4.isChecked()) stars = 4;
            else if (checkStar3.isChecked()) stars = 3;
            else if (checkStar2.isChecked()) stars = 2;
            else if (checkStar1.isChecked()) stars = 1;

            for (Map.Entry<CheckBox, String> entry : typeCheckBoxMap.entrySet()) {
                if (entry.getKey().isChecked()) {
                    typesSelected.add(entry.getValue());
                }
            }

            SearchRequest request = new SearchRequest(latitude, longitude, stars, typesSelected, prices);
            try {
                listener.onFiltersSelected(request);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            dismiss();
        });

        return view;
    }
}

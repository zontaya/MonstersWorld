/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sonlabs.monstersworld.ui.skulist;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sonlabs.monstersworld.R;
import com.sonlabs.monstersworld.ui.skulist.row.RowViewHolder;
import com.sonlabs.monstersworld.ui.skulist.row.SkuRowData;

import java.util.List;

/**
 * Adapter for a RecyclerView that shows SKU details for the app.
 * <p>
 *     Note: It's done fragment-specific logic independent and delegates control back to the
 *     specified handler (implemented inside StoreFragment in this example)
 * </p>
 */
public class StoreAdapter extends RecyclerView.Adapter<RowViewHolder>
        implements RowViewHolder.OnButtonClickListener {
    private List<SkuRowData> mListData;
    private StoreFragment storeFragment;

    public StoreAdapter(StoreFragment storeFragment) {
        this.storeFragment = storeFragment;
    }

    void updateData(List<SkuRowData> data) {
        mListData = data;
        notifyDataSetChanged();
    }

    @Override
    public RowViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_moster, parent, false);
        return new RowViewHolder(item, this);
    }

    @Override
    public void onBindViewHolder(RowViewHolder holder, int position) {
        SkuRowData data = getData(position);
        if (data != null) {
            holder.title.setText(data.getTitle());
            holder.description.setText(data.getDescription());
            holder.price.setText(data.getPrice());
            holder.button.setEnabled(true);
        }

        if (data != null) {
            switch (data.getSku()) {
                case "chewbacca":
                    holder.skuIcon.setImageResource(R.drawable.ic_chewbacca);
                    break;
                case "frankenstein":
                    holder.skuIcon.setImageResource(R.drawable.ic_frankenstein);
                    break;
                case "groot":
                    holder.skuIcon.setImageResource(R.drawable.ic_groot);
                    break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return mListData == null ? 0 : mListData.size();
    }

    @Override
    public void onButtonClicked(int position) {

        //หา sku จาก position ใน recyclerview
        SkuRowData data = getData(position);
        if (data != null) {

            //เรียกใช้งาน method ซื้อ
            storeFragment.startPurchaseFlow(data.getSku(), data.getBillingType());
        }
    }

    private SkuRowData getData(int position) {
        return mListData == null ? null : mListData.get(position);
    }
}


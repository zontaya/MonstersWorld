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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.BillingResponse;
import com.android.billingclient.api.BillingClient.SkuType;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.sonlabs.monstersworld.R;
import com.sonlabs.monstersworld.ui.skulist.row.SkuRowData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Displays a screen with various in-app purchase and subscription options
 */
public class StoreFragment extends Fragment implements PurchasesUpdatedListener {
    private static final String TAG = "StoreFragment";

    private RecyclerView mRecyclerView;
    private StoreAdapter mAdapter;
    private BillingClient mBillingClient;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //สร้าง BillingClient
        mBillingClient = BillingClient.newBuilder(getContext()).setListener(this).build();

        //start BillingClient
        startServiceConnection(null);
    }

    private void startServiceConnection(final Runnable executeOnSuccess) {
        if (mBillingClient.isReady()) {
            if (executeOnSuccess != null) {
                executeOnSuccess.run();
            }
        } else {
            //start BillingClient
            mBillingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(@BillingResponse int billingResponse) {
                    if (billingResponse == BillingResponse.OK) {
                        Log.i(TAG, "onBillingSetupFinished() response: " + billingResponse);
                        if (executeOnSuccess != null) {
                            executeOnSuccess.run();
                        }
                        //ถ้า connection response OK
                        handleManagerAndUiReady();

                    } else {
                        Log.w(TAG, "onBillingSetupFinished() error code: " + billingResponse);
                    }
                }
                @Override
                public void onBillingServiceDisconnected() {
                    Log.w(TAG, "onBillingServiceDisconnected()");
                }
            });
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.store_fragment, container, false);
        //สร้าง view สำหรับแสดงรายการสินค้า
        mRecyclerView = (RecyclerView) root.findViewById(R.id.list);
        onRecyclerViewReady();
        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBillingClient.endConnection();
    }

    public void onRecyclerViewReady() {
        //new adapter และส่ง BillingProvider เพิ่มใช้งาน event ตอนกดซื้อใน list
        if (mRecyclerView != null) {
            mAdapter = new StoreAdapter(this);
            if (mRecyclerView.getAdapter() == null) {
                mRecyclerView.setAdapter(mAdapter);
                mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            }
        }
    }

    private void handleManagerAndUiReady() {
        final List<SkuRowData> inList = new ArrayList<>();
        //เตรียม response จากการค้นหา
        SkuDetailsResponseListener responseListener = new SkuDetailsResponseListener() {
            @Override
            public void onSkuDetailsResponse(int responseCode, List<SkuDetails> skuDetailsList) {

                if (responseCode == BillingResponse.OK && skuDetailsList != null) {
                    //ดึงรายการสินค้าที่อยู่บน google play console
                    for (SkuDetails details : skuDetailsList) {
                        Log.i(TAG, "Found sku: " + details);
                        inList.add(new SkuRowData(details.getSku()
                                , details.getTitle()
                                , details.getPrice()
                                , details.getDescription()
                                , details.getType()));
                    }
                    //อัพเดทรายการสินค้าใน adapter
                    if (inList.size() > 0) {
                        mAdapter.updateData(inList);
                    }
                }
            }
        };
        // ค้นหารายการสินค้า
        String[] listSku = {"groot", "chewbacca", "frankenstein"};
        querySkuDetailsAsync(SkuType.INAPP, Arrays.asList(listSku), responseListener);
    }

    public void querySkuDetailsAsync(@BillingClient.SkuType final String itemType
            , final List<String> skuList
            , final SkuDetailsResponseListener listener) {
        // Specify a runnable to start when connection to Billing client is established
        Runnable executeOnConnectedService = new Runnable() {
            @Override
            public void run() {
                SkuDetailsParams skuDetailsParams = SkuDetailsParams.newBuilder().setSkusList(skuList).setType(itemType).build();
                mBillingClient.querySkuDetailsAsync(skuDetailsParams,
                        new SkuDetailsResponseListener() {
                            @Override
                            public void onSkuDetailsResponse(int responseCode, List<SkuDetails> skuDetailsList) {
                                listener.onSkuDetailsResponse(responseCode, skuDetailsList);
                            }
                        });
            }
        };
        // If Billing client was disconnected, we retry 1 time and if success, execute the query
        startServiceConnection(executeOnConnectedService);
    }

    public void startPurchaseFlow(final String skuId, final String billingType) {

        // Specify a runnable to start when connection to Billing client is established
        Runnable executeOnConnectedService = new Runnable() {
            @Override
            public void run() {
                BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                        .setType(billingType)
                        .setSku(skuId)
                        .build();
                mBillingClient.launchBillingFlow(getActivity(), billingFlowParams);
            }
        };

        // If Billing client was disconnected, we retry 1 time and if success, execute the query
        startServiceConnection(executeOnConnectedService);
    }


    @Override
    public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {
        Log.d(TAG, "onPurchasesUpdated() response: " + responseCode);
        if (responseCode == BillingResponse.OK) {
            Log.d(TAG, "onPurchasesUpdated response : success");
        }
    }
}


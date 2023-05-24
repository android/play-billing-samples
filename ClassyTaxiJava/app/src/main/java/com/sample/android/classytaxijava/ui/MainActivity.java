/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sample.android.classytaxijava.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseUser;
import com.sample.android.classytaxijava.Constants;
import com.sample.android.classytaxijava.R;
import com.sample.android.classytaxijava.SubApp;
import com.sample.android.classytaxijava.billing.BillingClientLifecycle;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final int HOME_PAGER_INDEX = 0;
    public static final int PREMIUM_PAGER_INDEX = 1;
    public static final int SETTINGS_PAGER_INDEX = 2;

    private static final String TAG = "MainActivity";
    private static final int RC_SIGN_IN = 0;
    private static final int COUNT = 3;

    private SectionsPagerAdapter sectionsPagerAdapter;
    private ViewPager container;
    private TabLayout tabs;

    private BillingClientLifecycle billingClientLifecycle;

    private FirebaseUserViewModel authenticationViewModel;
    private BillingViewModel billingViewModel;
    private SubscriptionStatusViewModel subscriptionViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        // Set up the ViewPager with the sections adapter.
        container = findViewById(R.id.container);
        tabs = findViewById(R.id.tabs);
        container.setAdapter(sectionsPagerAdapter);
        container.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabs));
        tabs.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(container));

        authenticationViewModel = new ViewModelProvider(this).get(FirebaseUserViewModel.class);
        billingViewModel = new ViewModelProvider(this).get(BillingViewModel.class);
        subscriptionViewModel = new ViewModelProvider(this).get(SubscriptionStatusViewModel.class);

        billingClientLifecycle = ((SubApp) getApplication()).getBillingClientLifecycle();
        getLifecycle().addObserver(billingClientLifecycle);

        // Register purchases when they change.
        billingClientLifecycle.purchaseUpdateEvent.observe(this, new Observer<List<Purchase>>() {
            @Override
            public void onChanged(List<Purchase> purchases) {
                if (purchases != null) {
                    registerPurchases(purchases);
                }
            }
        });

        // Launch billing flow when user clicks button to buy something.
        billingViewModel.buyEvent.observe(this, new Observer<BillingFlowParams>() {
            @Override
            public void onChanged(BillingFlowParams billingFlowParams) {
                if (billingFlowParams != null) {
                    billingClientLifecycle
                            .launchBillingFlow(MainActivity.this, billingFlowParams);
                }
            }
        });

        // Open the Play Store when event is triggered.
        billingViewModel.openPlayStoreSubscriptionsEvent.observe(this, new Observer<String>() {
            @Override
            public void onChanged(String sku) {
                Log.i(TAG, "Viewing subscriptions on the Google Play Store");
                String url;
                if (sku == null) {
                    // If the SKU is not specified, just open the Google Play subscriptions URL.
                    url = Constants.PLAY_STORE_SUBSCRIPTION_URL;
                } else {
                    // If the SKU is specified, open the deeplink for this SKU on Google Play.
                    url = String.format(Constants.PLAY_STORE_SUBSCRIPTION_DEEPLINK_URL,
                            sku, getApplicationContext().getPackageName());
                }
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            }
        });

        // Update authentication UI.
        final Observer<FirebaseUser> fireaseUserObserver = new Observer<FirebaseUser>() {
            @Override
            public void onChanged(@Nullable final FirebaseUser firebaseUser) {
                invalidateOptionsMenu();
                if (firebaseUser == null) {
                    triggerSignIn();
                } else {
                    Log.d(TAG, "Current user: "
                            + firebaseUser.getEmail() + " " + firebaseUser.getDisplayName());
                }
            }
        };
        authenticationViewModel.firebaseUser.observe(this, fireaseUserObserver);

        // Update subscription information when user changes.
        authenticationViewModel.userChangeEvent.observe(this, new Observer<Void>() {
            @Override
            public void onChanged(Void aVoid) {
                subscriptionViewModel.userChanged();
                List<Purchase> purchases = billingClientLifecycle.purchaseUpdateEvent.getValue();
                if (purchases != null) {
                    registerPurchases(purchases);
                }
            }
        });
    }

    /**
     * Register SKUs and purchase tokens with the server.
     */
    private void registerPurchases(List<Purchase> purchaseList) {
        for (Purchase purchase : purchaseList) {
            String sku = purchase.getSkus().get(0);
            String purchaseToken = purchase.getPurchaseToken();
            Log.d(TAG, "Register purchase with sku: " + sku + ", token: " + purchaseToken);
            subscriptionViewModel.registerSubscription(sku, purchaseToken);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    /**
     * Update menu based on sign-in state. Called in response to {@link #invalidateOptionsMenu}.
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean isSignedIn = authenticationViewModel.isSignedIn();
        menu.findItem(R.id.sign_in).setVisible(!isSignedIn);
        menu.findItem(R.id.sign_out).setVisible(isSignedIn);
        return true;
    }

    /**
     * Called when menu item is selected.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out:
                triggerSignOut();
                return true;
            case R.id.sign_in:
                triggerSignIn();
                return true;
            case R.id.refresh:
                refreshData();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void refreshData() {
        billingClientLifecycle.queryPurchases();
        subscriptionViewModel.manualRefresh();
    }

    /**
     * Sign in with FirebaseUI Auth.
     */
    private void triggerSignIn() {
        Log.d(TAG, "Attempting SIGN-IN!");
        List<AuthUI.IdpConfig> providers = new ArrayList<>();
        // Configure the different methods users can sign in
        providers.add(new AuthUI.IdpConfig.EmailBuilder().build());
        providers.add(new AuthUI.IdpConfig.GoogleBuilder().build());

        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN);
    }

    /**
     * Sign out with FirebaseUI Auth.
     */
    private void triggerSignOut() {
        subscriptionViewModel.unregisterInstanceId();
        AuthUI.getInstance().signOut(this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.d(TAG, "User SIGNED OUT!");
                        authenticationViewModel.updateFirebaseUser();
                    }
                });
    }

    /**
     * Receive Activity result, including sign-in result.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            // If sign-in is successful, update ViewModel.
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "Sign-in SUCCESS!");
                authenticationViewModel.updateFirebaseUser();
            } else {
                Log.d(TAG, "Sign-in FAILED!");
            }
        } else {
            Log.e(TAG, "Unrecognized request code: " + requestCode);
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            return TabFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return COUNT;
        }
    }
}

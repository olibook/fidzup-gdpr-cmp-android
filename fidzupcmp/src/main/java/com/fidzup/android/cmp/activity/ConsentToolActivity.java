package com.fidzup.android.cmp.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.fidzup.android.cmp.R;
import com.fidzup.android.cmp.manager.ConsentManager;
import com.fidzup.android.cmp.model.ConsentToolConfiguration;

/**
 * Consent tool activity.
 */

public class ConsentToolActivity extends ConsentActivity {

    static private final int PREFERENCES_REQUEST_CODE = 0;
    static private final int VENDOR_REQUEST_CODE = 1;

    private RecyclerView recyclerView;
    private Adapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.consent_tool_activity_layout);
        recyclerView = findViewById(R.id.consentToolRecyclerView);


        /*
        // Setup main logo
        ImageView mainLogoImageView = findViewById(R.id.fidzup_logo);
        mainLogoImageView.setImageResource(config.getHomeScreenLogoDrawableRes());

        // Setup main text
        TextView mainTextView = findViewById(R.id.main_textview);
        mainTextView.setText(config.getHomeScreenText());

        // Setup the accept and close button
        Button closeButton = findViewById(R.id.close_button);
        closeButton.setText(config.getHomeScreenCloseButtonTitle());
        closeButton.getBackground().setColorFilter(getResources().getColor(R.color.actionButtonColor), PorterDuff.Mode.MULTIPLY);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Accept all new vendors or purposes.
                ConsentManager.getSharedInstance().allowAllPurposes();
                finish();
            }
        });

        // Setup the refuse & close button
        Button closeRefuseButton = findViewById(R.id.close_refuse_button);
        closeRefuseButton.setText(config.getHomeScreenCloseRefuseButtonTitle());
        closeRefuseButton.getBackground().setColorFilter(getResources().getColor(R.color.actionButtonColor), PorterDuff.Mode.MULTIPLY);
        closeRefuseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Refuse all new vendors or purposes.
                ConsentManager.getSharedInstance().revokeAllPurposes();
                finish();
            }
        });

        // Setup the manage consent button
        Button manageButton = findViewById(R.id.manage_button);
        manageButton.setText(config.getHomeScreenManageConsentButtonTitle());
        manageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Start the ConsentToolPreferencesActivity
                Intent intent = getIntentForConsentActivity(ConsentToolPreferencesActivity.class,
                        getConsentStringFromIntent(),
                        getVendorListFromIntent(),
                        getEditorFromIntent());
                startActivityForResult(intent, PREFERENCES_REQUEST_CODE);
            }
        });

        */

        adapter = new Adapter(ConsentManager.getSharedInstance().getConsentToolConfiguration());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == VENDOR_REQUEST_CODE) return;
        if (resultCode != RESULT_OK) return;

        // save the new consent string and close activity
        storeConsentString(getResultConsentString(data));
        finish();
    }

    @Override
    public void onBackPressed() {
        if(isRootConsentActivity()) { // this activity should always be root but checking anyway

            // catch the back button pressed event.
            // Show alert that warns the user, and force it to click on buttons to quit.
            ConsentToolConfiguration config = ConsentManager.getSharedInstance().getConsentToolConfiguration();
            new AlertDialog.Builder(this)
                    .setTitle(config.getConsentManagementAlertDialogTitle())
                    .setMessage(config.getConsentManagementAlertDialogText())
                    .setPositiveButton(config.getConsentManagementAlertDialogPositiveButtonTitle(), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Return the initial consent string.
                            ConsentToolActivity.this.storeConsentString(getConsentStringFromIntent());
                            ConsentToolActivity.super.onBackPressed();
                        }
                    })
                    .setNegativeButton(config.getConsentManagementAlertDialogNegativeButtonTitle(), null)
                    .show();
        }
    }

    public void onVendorListClick(View view) {
        // Start the VendorListActivity
        Intent intent = getIntentForConsentActivity(VendorListActivity.class,
                getConsentStringFromIntent(),
                getVendorListFromIntent(),
                getEditorFromIntent());
        intent.putExtra(VendorListActivity.EXTRA_READONLY, true);
        startActivityForResult(intent, VENDOR_REQUEST_CODE);
    }

    private static class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int VIEW_TYPE_HEADER = 0;
        private static final int VIEW_TYPE_PURPOSE = 1;
        private static final int VIEW_TYPE_FOOTER = 2;
        private final ConsentToolConfiguration config;

        private Adapter(ConsentToolConfiguration config) {
            super();
            this.config = config;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            switch (viewType) {
                case VIEW_TYPE_HEADER:
                    View v = LayoutInflater.from(parent.getContext())
                          .inflate(R.layout.header_cell, parent, false);
                    return new HeaderViewHolder(v);
                case VIEW_TYPE_FOOTER:
                    throw new RuntimeException("not implemented");
                case VIEW_TYPE_PURPOSE:
                    throw new RuntimeException("not implemented");
            }
            throw new AssertionError(String.format("unexpected view type {}", viewType));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (HeaderViewHolder.class.isInstance(holder)) {
                HeaderViewHolder vh = (HeaderViewHolder) holder;

                vh.mainLogoImageView.setImageResource(config.getHomeScreenLogoDrawableRes());
                vh.mainTextView.setText(config.getHomeScreenText());
            }
        }

        @Override
        public int getItemCount() {
            return 1;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return VIEW_TYPE_HEADER;
            }
            if (position == getItemCount() - 1) {
                return VIEW_TYPE_FOOTER;
            }
            if (position > 0 && position < getItemCount() - 1) {
                return VIEW_TYPE_PURPOSE;
            }
            throw new AssertionError(String.format("Unexpected position {}", position));
        }
    }

    private static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView mainTextView;
        private final ImageView mainLogoImageView;

        private HeaderViewHolder(View itemView) {
            super(itemView);
            mainLogoImageView = itemView.findViewById(R.id.fidzup_logo);
            mainTextView = itemView.findViewById(R.id.main_textview);
        }
    }
}

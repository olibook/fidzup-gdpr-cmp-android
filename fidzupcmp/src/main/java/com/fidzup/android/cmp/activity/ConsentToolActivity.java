package com.fidzup.android.cmp.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.fidzup.android.cmp.R;
import com.fidzup.android.cmp.consentstring.ConsentString;
import com.fidzup.android.cmp.manager.ConsentManager;
import com.fidzup.android.cmp.model.ConsentToolConfiguration;
import com.fidzup.android.cmp.model.Purpose;
import com.fidzup.android.cmp.model.VendorList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Consent tool activity.
 */

public class ConsentToolActivity extends ConsentActivity {

    static private final int PREFERENCES_REQUEST_CODE = 0;
    static private final int VENDOR_REQUEST_CODE = 1;

    private RecyclerView recyclerView;
    private Adapter adapter;
    private ConsentString consentString;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.consent_tool_activity_layout);
        recyclerView = findViewById(R.id.consentToolRecyclerView);

        consentString = getConsentStringFromIntent();
        VendorList vl =  getVendorListFromIntent();
        List<Purpose> purposes = vl == null ? Collections.<Purpose>emptyList() : vl.getPurposes();
        adapter = new Adapter(ConsentManager.getSharedInstance().getConsentToolConfiguration(), purposes);
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
                            ConsentToolActivity.this.storeConsentString(consentString);
                            ConsentToolActivity.super.onBackPressed();
                        }
                    })
                    .setNegativeButton(config.getConsentManagementAlertDialogNegativeButtonTitle(), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ConsentManager.getSharedInstance().revokeAllPurposes();
                            ConsentToolActivity.super.onBackPressed();
                        }
                    })
                    .show();
        }
    }

    private class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int VIEW_TYPE_HEADER = 0;
        private static final int VIEW_TYPE_PURPOSE = 1;
        private static final int VIEW_TYPE_FOOTER = 2;
        private final @NonNull ConsentToolConfiguration config;
        private final @NonNull List<Purpose> purposes;

        private @NonNull List<Purpose> customPurposes = new ArrayList<>();

        private Adapter(ConsentToolConfiguration config, List<Purpose> purposes) {
            super();
            this.config = config;
            this.purposes = purposes;
            for (Purpose p: purposes) {
                if (consentString.isPurposeAllowed(p.getId())) {
                    customPurposes.add(p);
                }
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            switch (viewType) {
                case VIEW_TYPE_HEADER:
                    View headerView = LayoutInflater.from(parent.getContext())
                          .inflate(R.layout.cmp_header_cell, parent, false);
                    return new HeaderViewHolder(headerView);
                case VIEW_TYPE_FOOTER:
                    View footerView = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.cmp_footer_cell, parent, false);
                    return new FooterViewHolder(footerView);
                case VIEW_TYPE_PURPOSE:
                    View purposeView = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.cmp_purpose_cell, parent, false);
                    return new PurposeViewHolder(purposeView);
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
            if (PurposeViewHolder.class.isInstance(holder)) {
                PurposeViewHolder vh = (PurposeViewHolder) holder;
                final Purpose p = purposes.get(position - 1);
                vh.titleView.setText(p.getName());
                vh.detailView.setText(p.getDescription());
                vh.statusSwitch.setChecked(consentString.isPurposeAllowed(p.getId()));
                vh.statusSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            customPurposes.add(p);
                            consentString = ConsentString.consentStringByAddingPurposeConsent(p.getId(), consentString);
                        } else {
                            customPurposes.remove(p);
                            consentString = ConsentString.consentStringByRemovingPurposeConsent(p.getId(), consentString);
                        }
                        int footerCell = getItemCount() - 1;
                        notifyItemChanged(footerCell);
                    }
                });
            }
            if (FooterViewHolder.class.isInstance(holder)) {
                FooterViewHolder vh = (FooterViewHolder) holder;
                vh.vendorListTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = getIntentForConsentActivity(VendorListActivity.class,
                                consentString,
                                getVendorListFromIntent(),
                                getEditorFromIntent());
                        intent.putExtra(VendorListActivity.EXTRA_READONLY, true);
                        startActivityForResult(intent, VENDOR_REQUEST_CODE);
                    }
                });

                vh.acceptButton.setVisibility(customPurposes.isEmpty() ? View.VISIBLE : View.GONE);
                vh.acceptButton.setText(config.getHomeScreenCloseButtonTitle());
                vh.acceptButton.getBackground().setColorFilter(getResources().getColor(R.color.actionButtonColor), PorterDuff.Mode.MULTIPLY);
                vh.acceptButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Accept all new vendors or purposes.
                        ConsentManager.getSharedInstance().allowAllPurposes();
                        finish();
                    }
                });

                vh.refuseButton.setVisibility(customPurposes.isEmpty() ? View.VISIBLE : View.GONE);
                vh.refuseButton.setText(config.getHomeScreenCloseRefuseButtonTitle());
                vh.refuseButton.getBackground().setColorFilter(getResources().getColor(R.color.actionButtonColor), PorterDuff.Mode.MULTIPLY);
                vh.refuseButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Refuse all new vendors or purposes.
                        ConsentManager.getSharedInstance().revokeAllPurposes();
                        finish();
                    }
                });

                vh.saveButton.setVisibility(customPurposes.isEmpty() ? View.GONE : View.VISIBLE);
                vh.saveButton.setText(ConsentManager.getSharedInstance().getConsentToolConfiguration().getConsentManagementSaveButtonTitle());
                vh.saveButton.getBackground().setColorFilter(getResources().getColor(R.color.actionButtonColor), PorterDuff.Mode.MULTIPLY);
                vh.saveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        storeConsentString(consentString);
                        finish();
                    }
                });


                vh.manageButton.setText(config.getHomeScreenManageConsentButtonTitle());
                vh.manageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Start the ConsentToolPreferencesActivity
                        Intent intent = getIntentForConsentActivity(ConsentToolPreferencesActivity.class,
                                consentString,
                                getVendorListFromIntent(),
                                getEditorFromIntent());
                        startActivityForResult(intent, PREFERENCES_REQUEST_CODE);
                    }
                });

            }
        }

        @Override
        public int getItemCount() {
            return purposes.size() + 2;
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
        private final @NonNull TextView mainTextView;
        private final @NonNull ImageView mainLogoImageView;

        private HeaderViewHolder(View itemView) {
            super(itemView);
            mainLogoImageView = itemView.findViewById(R.id.fidzup_logo);
            mainTextView = itemView.findViewById(R.id.main_textview);
        }
    }

    private static class FooterViewHolder extends RecyclerView.ViewHolder {
        private final @NonNull TextView vendorListTextView;
        private final @NonNull Button acceptButton;
        private final @NonNull Button refuseButton;
        private final @NonNull Button manageButton;
        private final @NonNull Button saveButton;

        private FooterViewHolder(View itemView) {
            super(itemView);
            vendorListTextView = itemView.findViewById(R.id.vendorList);
            acceptButton = itemView.findViewById(R.id.close_button);
            refuseButton = itemView.findViewById(R.id.close_refuse_button);
            manageButton = itemView.findViewById(R.id.manage_button);
            saveButton = itemView.findViewById(R.id.save_button);
        }
    }

    private static class PurposeViewHolder extends RecyclerView.ViewHolder {
        private final @NonNull TextView titleView;
        private final @NonNull TextView detailView;
        private final @NonNull SwitchCompat statusSwitch;
        private boolean expanded;
        private PurposeViewHolder(View itemView) {
            super(itemView);
            expanded = false;
            titleView = itemView.findViewById(R.id.cmp_purpose_cell_title);
            detailView = itemView.findViewById(R.id.cmp_purpose_cell_detail);
            statusSwitch = itemView.findViewById(R.id.cmp_purpose_switch);
            detailView.setVisibility(View.GONE);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    expand(!expanded);
                }
            });
        }

        void expand(boolean expand) {
            if (expand && ! expanded) {
                expanded = expand;
                detailView.setVisibility(View.VISIBLE);
            }
            if (!expand && expanded) {
                expanded = expand;
                detailView.setVisibility(View.GONE);
            }

        }

    }
}

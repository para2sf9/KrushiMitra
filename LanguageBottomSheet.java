package com.example.ai_agri;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;

public class LanguageBottomSheet extends BottomSheetDialog {

    private ImageView checkEnglish, checkHindi, checkMarathi;
    private Context context;
    private OnLanguageSelectedListener listener;

    public interface OnLanguageSelectedListener {
        void onLanguageSelected(String langCode);
    }

    public LanguageBottomSheet(@NonNull Context context, OnLanguageSelectedListener listener) {
        super(context);
        this.context = context;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = getLayoutInflater().inflate(R.layout.language_bottom_sheet, null);
        setContentView(view);

        checkEnglish = view.findViewById(R.id.check_english);
        checkHindi = view.findViewById(R.id.check_hindi);
        checkMarathi = view.findViewById(R.id.check_marathi);

        // Show current language
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String currentLang = prefs.getString("app_language", "en");
        updateCheckmarks(currentLang);

        // Set click listeners
        view.findViewById(R.id.lang_english).setOnClickListener(v -> setLanguage("en"));
        view.findViewById(R.id.lang_hindi).setOnClickListener(v -> setLanguage("hi"));
        view.findViewById(R.id.lang_marathi).setOnClickListener(v -> setLanguage("mr"));
    }

    private void setLanguage(String langCode) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString("app_language", langCode).apply();

        updateCheckmarks(langCode);

        if (listener != null) {
            listener.onLanguageSelected(langCode);
        }
        dismiss();
    }

    private void updateCheckmarks(String langCode) {
        checkEnglish.setVisibility("en".equals(langCode) ? View.VISIBLE : View.GONE);
        checkHindi.setVisibility("hi".equals(langCode) ? View.VISIBLE : View.GONE);
        checkMarathi.setVisibility("mr".equals(langCode) ? View.VISIBLE : View.GONE);
    }
}

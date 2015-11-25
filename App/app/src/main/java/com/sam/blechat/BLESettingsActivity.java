package com.sam.blechat;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;

public class BLESettingsActivity extends Activity {

    public static final String PREF_KEY_USERNAME = "username";

    public static class BLESettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new BLESettingsFragment())
                .commit();
    }
}
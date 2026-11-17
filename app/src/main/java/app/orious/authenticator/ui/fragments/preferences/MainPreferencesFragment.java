package app.orious.authenticator.ui.fragments.preferences;

import android.os.Bundle;

import app.orious.authenticator.R;

public class MainPreferencesFragment extends PreferencesFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);
    }
}

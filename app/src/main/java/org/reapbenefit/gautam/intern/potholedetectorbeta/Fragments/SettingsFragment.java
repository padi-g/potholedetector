package org.reapbenefit.gautam.intern.potholedetectorbeta.Fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.reapbenefit.gautam.intern.potholedetectorbeta.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
           setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}

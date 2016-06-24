package com.framgia.youralarm1.activity;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;
import com.framgia.youralarm1.R;
import com.framgia.youralarm1.contstant.Const;
import java.util.List;

public class SettingsActivity extends AppCompatPreferenceActivity {

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener =
            new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object value) {
                    String stringValue = value.toString();

                    if (preference instanceof ListPreference) {
                        ListPreference listPreference = (ListPreference) preference;
                        int index = listPreference.findIndexOfValue(stringValue);
                        if (index >= 0) {
                            preference.setSummary(listPreference.getEntries()[index]);
                        } else {
                            preference.setSummary(listPreference.getEntries()[0]);
                            ((ListPreference) preference).setValueIndex(0);
                        }

                    } else if (preference instanceof RingtonePreference) {
                        if (TextUtils.isEmpty(stringValue)) {
                            Context context = preference.getContext();
                            Uri currenturi =
                                    RingtoneManager.getActualDefaultRingtoneUri(context,
                                                                                RingtoneManager.TYPE_ALARM);
                            String ringToneName =
                                    RingtoneManager.getRingtone(context, currenturi)
                                            .getTitle(context);
                            preference.setSummary(ringToneName);

                        } else {
                            Ringtone ringtone;
                            if (!stringValue.equals(String.valueOf(Settings.System.DEFAULT_ALARM_ALERT_URI))) {
                                ringtone = RingtoneManager.getRingtone(preference.getContext(),
                                                                       Uri.parse(stringValue));
                                String name = ringtone.getTitle(preference.getContext());
                                preference.setSummary(name);
                            } else {
                                Context context = preference.getContext();
                                Uri currenturi =
                                        RingtoneManager.getActualDefaultRingtoneUri(context,
                                                                                    RingtoneManager.TYPE_RINGTONE);
                                String ringToneName =
                                        RingtoneManager.getRingtone(context, currenturi)
                                                .getTitle(context);
                                ringToneName += Const.DEFAULT;
                                preference.setSummary(ringToneName);
                            }
                        }

                    } else {
                        preference.setSummary(stringValue);
                    }
                    return true;
                }
            };

    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        String tempString = PreferenceManager.getDefaultSharedPreferences(preference.getContext())
                                                .getString(preference.getKey(), "");
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, tempString);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName) ||
                SnoozePreferenceFragment.class.getName().equals(fragmentName) ||
                SoundFragment.class.getName().equals(fragmentName);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SnoozePreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_snooze);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_snooze_time)));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SoundFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_sound);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_alarm_sound)));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}

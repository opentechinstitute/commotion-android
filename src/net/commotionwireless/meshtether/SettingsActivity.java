/*
 *  This file is part of Commotion Mesh Tether
 *  Copyright (C) 2010 by Szymon Jakubczak
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.commotionwireless.meshtether;

import net.commotionwireless.meshtether.R;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
    final static int[] prefids = {
        R.string.if_lan, R.string.if_wan,
        R.string.adhoc_ip, R.string.adhoc_netmask, R.string.adhoc_dns_server, R.string.lan_essid, R.string.lan_bssid, R.string.lan_channel,
        R.string.lan_script
    };
    final static int[] checks = { R.string.lan_wext };

    private void setSummary(Preference p, CharSequence s) {
        if ((s != null) && (s.length() > 0)) {
            p.setSummary(getString(R.string.current) + s);
        } else {
            p.setSummary(null);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        for (int i = 0; i < prefids.length; ++i) {
            Preference pref = findPreference(getString(prefids[i]));
            pref.setOnPreferenceChangeListener(this);
            if (ListPreference.class.isInstance(pref)) {
                ListPreference preference = (ListPreference) pref;
                setSummary(preference, preference.getValue());
            } else if (EditTextPreference.class.isInstance(pref)) {
                EditTextPreference preference = (EditTextPreference) pref;
                setSummary(preference, preference.getText());
            }
        }
        for (int i = 0; i < checks.length; ++i) {
            Preference pref = findPreference(getString(checks[i]));
            pref.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        String key = pref.getKey();
        if (key == null) return true;

        if (((MeshTetherApp)getApplication()).isRunning()) {
            Toast.makeText(this, getString(R.string.restartneeded), Toast.LENGTH_SHORT).show();
        }

        if (ListPreference.class.isInstance(pref) || EditTextPreference.class.isInstance(pref)) {
            setSummary(pref, (String)newValue);
        } // else don't update summary
        return true;
    }
}

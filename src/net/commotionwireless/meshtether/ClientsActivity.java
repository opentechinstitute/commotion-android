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

import java.util.ArrayList;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

public class ClientsActivity extends android.app.ListActivity {
	private MeshTetherApp app;
	private BaseAdapter adapter;
	private ArrayList<MeshService.ClientData> clients = new ArrayList<MeshService.ClientData>();

	private static class ViewHolder {
		TextView remoteIP;
		ProgressBar lqBar;
		ProgressBar nlqBar;
		RadioButton hnaIndicator;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (MeshTetherApp)getApplication();
		app.setClientsActivity(this);

		adapter = new BaseAdapter(){
			public int getCount() { return clients.size(); }
			public Object getItem(int position) { return clients.get(position); }
			public long getItemId(int position) { return position; }

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				final MeshService.ClientData client = clients.get(position);

				ViewHolder holder;

				if (convertView == null) {
					View view = getLayoutInflater().inflate(R.layout.linkrow, null);
					holder = new ViewHolder();
					holder.remoteIP = (TextView) view.findViewById(R.id.remoteip);
					holder.lqBar = (ProgressBar) view.findViewById(R.id.linkquality);
					holder.nlqBar = (ProgressBar) view.findViewById(R.id.neighborlinkquality);
					holder.hnaIndicator = (RadioButton) view.findViewById(R.id.hna);
					view.setTag(holder);
					view.setClickable(false);
					convertView = view;
				} else {
					holder = (ViewHolder) convertView.getTag();
				}

				holder.remoteIP.setText(client.remoteIP);
				holder.lqBar.setProgress((int)(client.linkQuality * 100));
				holder.nlqBar.setProgress((int)(client.neighborLinkQuality * 100));
				holder.hnaIndicator.setChecked(client.hasHna);
				return convertView;
			}
		};
		setListAdapter(adapter);
		setTitle(getString(R.string.clientview));
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		app.setClientsActivity(null);
	}

	@Override
	protected void onResume() {
		super.onResume();
		app.cancelClientNotify();
		update();

		if (hasWindowFocus() && clients.isEmpty())
			app.updateToast(getString(R.string.noclients), false);
	}

	public void update() {
		if (app.service != null)
			clients = app.service.clients;
		adapter.notifyDataSetChanged();
	}
}

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

import net.commotionwireless.olsrd.ClientData;
import net.commotionwireless.olsrd.OlsrdService;
import net.commotionwireless.olsrinfo.datatypes.HNA;
import net.commotionwireless.olsrinfo.datatypes.Link;
import net.commotionwireless.olsrinfo.datatypes.OlsrDataDump;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class LinksActivity extends android.app.ListActivity {
	private MeshTetherApp app;
	private BaseAdapter adapter;
	private ArrayList<ClientData> clients = new ArrayList<ClientData>();

	private OlsrInfoThread mOlsrInfoThread;
	boolean mPauseOlsrInfoThread = false;
	private final Handler mHandler = new Handler();
	private BroadcastReceiver mOlsrdStatusReceiver;

	private static class ViewHolder {
		TextView remoteIP;
		ProgressBar lqBar;
		ProgressBar nlqBar;
		LinearLayout idrow;
		ImageView defaultRouteIcon;
		ImageView otherRouteIcon;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (MeshTetherApp)getApplication();
		app.setLinksActivity(this);

		adapter = new BaseAdapter(){
			@Override
			public int getCount() { return clients.size(); }
			@Override
			public Object getItem(int position) { return clients.get(position); }
			@Override
			public long getItemId(int position) { return position; }

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				final ClientData client = clients.get(position);

				ViewHolder holder;

				if (convertView == null) {
					View view = getLayoutInflater().inflate(R.layout.linkrow, null);
					holder = new ViewHolder();
					holder.remoteIP = (TextView) view.findViewById(R.id.remoteip);
					holder.lqBar = (ProgressBar) view.findViewById(R.id.linkquality);
					holder.nlqBar = (ProgressBar) view.findViewById(R.id.neighborlinkquality);
					holder.idrow = (LinearLayout) view.findViewById(R.id.idrow);
					holder.defaultRouteIcon = new ImageView(getBaseContext());
					holder.defaultRouteIcon.setId(R.drawable.default_route);
					holder.defaultRouteIcon.setImageResource(R.drawable.default_route);
					holder.otherRouteIcon = new ImageView(getBaseContext());
					holder.otherRouteIcon.setId(R.drawable.other_route);
					holder.otherRouteIcon.setImageResource(R.drawable.other_route);
					view.setTag(holder);
					view.setClickable(false);
					convertView = view;
				} else {
					holder = (ViewHolder) convertView.getTag();
				}

				holder.remoteIP.setText(client.remoteIP);
				holder.lqBar.setProgress((int)(client.linkQuality * 100));
				holder.nlqBar.setProgress((int)(client.neighborLinkQuality * 100));

				if (client.hasDefaultRoute) {
					if(holder.idrow.findViewById(R.drawable.default_route) == null)
						holder.idrow.addView(holder.defaultRouteIcon);
				} else {
					holder.idrow.removeView(holder.defaultRouteIcon);
				}
				if (client.hasRouteToOther) {
					if(holder.idrow.findViewById(R.drawable.other_route) == null)
						holder.idrow.addView(holder.otherRouteIcon);
				} else {
					holder.idrow.removeView(holder.otherRouteIcon);
				}
				return convertView;
			}
		};
		setListAdapter(adapter);
		setTitle(getString(R.string.clientview));

		mOlsrdStatusReceiver = new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {
				if (intent.getAction() == OlsrdService.OLSRD_CHANGE_ACTION) {
					OlsrdService olsrdService = app.getOlsrdService();
					if (olsrdService.isOlsrdRunning()) {
						mOlsrInfoThread = new OlsrInfoThread();
						mOlsrInfoThread.start();
					} else {
						if (mOlsrInfoThread != null) {
							mOlsrInfoThread.interrupt();
							mOlsrInfoThread = null;
						}
					}
				}
			}
		};
		OlsrdService olsrdService = app.getOlsrdService();
		if (olsrdService != null && olsrdService.isOlsrdRunning()) {
			mOlsrInfoThread = new OlsrInfoThread();
			mOlsrInfoThread.start();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mOlsrInfoThread != null) {
			mOlsrInfoThread.interrupt();
			mOlsrInfoThread = null;
		}
		app.setLinksActivity(null);
	}

	@Override
	protected void onResume() {
		super.onResume();
		app.cancelClientNotify();
		update();

		if (hasWindowFocus() && clients.isEmpty())
			app.updateToast(getString(R.string.noclients), false);
		IntentFilter filter = new IntentFilter();
		filter.addAction(OlsrdService.OLSRD_CHANGE_ACTION);
		registerReceiver(mOlsrdStatusReceiver, filter);
	}

	@Override
	protected void onPause() {
		super.onPause();
		//mPauseOlsrInfoThread = true;
		unregisterReceiver(mOlsrdStatusReceiver);
	}

	public void update() {
		/*
		 * FIXME
		 */
		adapter.notifyDataSetChanged();

	}


	class OlsrInfoThread extends Thread {

		@Override
		public void run() {
			ArrayList<ClientData> clientsToAdd = new ArrayList<ClientData>();
			try {
				Log.i("LinksActivity", "Starting OlsrdInfoThread()");
				while(!Thread.interrupted()) {
					/*
					 * FIXME
					 */

					OlsrDataDump dump = app.mJsonInfo.parseCommand("/links/hna");
					for (Link l : dump.links) {
						ClientData c = new ClientData(l.remoteIP, l.linkQuality,
								l.neighborLinkQuality, l.linkCost, l.validityTime);
						for (HNA h : dump.hna) {
							if (l.remoteIP.equals(h.gateway))
								if (h.genmask == 0)
									c.hasDefaultRoute = true;
								else
									c.hasRouteToOther = true;
						}
						clientsToAdd.add(c);
					}
					final ArrayList<ClientData> updateList = new ArrayList<ClientData>(clientsToAdd);
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							for (ClientData cd : updateList) {
								clientAdded(cd);
							}
						}
					});
					while (mPauseOlsrInfoThread)
						Thread.sleep(500);
					Thread.sleep(5000);
				}
			} catch (InterruptedException e) {
				// fall through
			}
			Log.i("LinksActivity", "Stopping OlsrdInfoThread()");
		}
	}

	private void clientAdded(ClientData cd) {

		for (int i = 0; i < clients.size(); ++i) {
			ClientData c = clients.get(i);
			if (c.remoteIP.equals(cd.remoteIP)) {
				if (c.hasRouteToOther == cd.hasRouteToOther
						&& c.hasDefaultRoute == cd.hasDefaultRoute
						&& c.linkQuality == cd.linkQuality
						&& c.neighborLinkQuality == cd.neighborLinkQuality) {
					return; // no change
				}
				clients.remove(i); // we'll add it at the end
				break;
			}
		}
		clients.add(cd);
		app.clientAdded(cd);
	}

}

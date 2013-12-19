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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

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
import android.text.method.LinkMovementMethod;
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
			public int getCount() { return (clients.size() == 0) ? 1 : clients.size(); }
			@Override
			public Object getItem(int position) { return clients.get(position); /*return (clients.get(position) == null) ? new Object() : clients.get(position); */ }
			@Override
			public long getItemId(int position) { return position; }

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				final ClientData client;


				ViewHolder holder;

				if (clients.isEmpty()) {
					/*
					 * we want to simply display the About text.
					 */
					View view = getLayoutInflater().inflate(R.layout.welcome, null);
					TextView tv = (TextView)view.findViewById(R.id.welcome_text_id);
					tv.setMovementMethod(LinkMovementMethod.getInstance());
					convertView = view;

					return convertView;
				}
				client = clients.get(position);
				if (convertView == null || convertView.getTag() == null) {
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
				if (intent.getAction().equalsIgnoreCase(OlsrdService.OLSRD_CHANGE_ACTION)) {
					OlsrdService olsrdService = app.getOlsrdService();
					if (olsrdService != null && olsrdService.isOlsrdRunning()) {
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
		OlsrdService olsrdService = app.getOlsrdService();

		app.cancelClientNotify();

		if (olsrdService != null) {
			if (olsrdService.isOlsrdRunning()) {
				/*
				 * service is running. Start a monitor
				 * thread if one doesn't already exist.
				 */
				if (mOlsrInfoThread == null) {
					mOlsrInfoThread = new OlsrInfoThread();
					mOlsrInfoThread.start();
				}
			} else {
				/*
				 * service is not running. Stop the
				 * monitor thread if it exists.
				 */
				if (mOlsrInfoThread != null) {
					mOlsrInfoThread.interrupt();
					mOlsrInfoThread = null;
				}
			}
		}

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
		adapter.notifyDataSetChanged();
	}


	class OlsrInfoThread extends Thread {

		@Override
		public void run() {
			int maxIoExceptions = 10;
			int ioExceptionCount = 0;
			try {
				Log.i("LinksActivity", "Starting OlsrdInfoThread()");
				while(!Thread.interrupted()) {
					ArrayList<ClientData> clientsToAdd = new ArrayList<ClientData>();

					/*
					 * Add 1 to the missed update counter for
					 * all clients. We decrement it later in
					 * clientAdded() if it is found.
					 */
					for (ClientData c : clients ) {
						c.missedUpdateCounter = c.missedUpdateCounter+1;
					}

					OlsrDataDump dump = null;
					try {
						dump = app.mJsonInfo.parseCommand("/links/hna");
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

							/*
							 * Reset the missed update counter since
							 * we didn't miss them on this iteration.
							 */
							if (clients.contains(c)) {
								ClientData cd = clients.get(clients.indexOf(c));
								Log.i("LinksActivity", "Resetting missedUpdateCounter for " + cd.remoteIP);
								cd.missedUpdateCounter = 0;
							}
						}
						final ArrayList<ClientData> updateList = new ArrayList<ClientData>(clientsToAdd);
						mHandler.post(new Runnable() {
							@Override
							public void run() {
								for (ClientData cd : updateList) {
									clientAdded(cd);
								}
								updateList.clear();
							}
						});
						ioExceptionCount = 0;
					} catch (IOException e) {
						if (ioExceptionCount++ > maxIoExceptions) {
							Log.w("LinksActivity", "Too many missed connections for JsonInfo plugin.");
							interrupt();
						}
					}

					while (mPauseOlsrInfoThread)
						Thread.sleep(500);
					Thread.sleep(5000);

					/*
					 * Remove the clients whose missed update
					 * counter exceeds the limit.
					 */
					synchronized (clients) {
						Iterator<ClientData> i = clients.iterator();
						while (i.hasNext()) {
							ClientData c = i.next();
							if (c.shouldRemove()) {
								Log.i("LinksActivity", "Removing " + c.remoteIP + " from the UI after too many missed updates.");
								i.remove();
							}
						}
					}
					mHandler.post(new Runnable() {
						public void run() {
							update();
						}
					});
				}
			} catch (InterruptedException e) {
				// fall through
			}
			Log.i("LinksActivity", "Stopping OlsrdInfoThread()");
			clients.clear();
			mHandler.post(new Runnable() {
				public void run() {
					update();
				}
			});
		}
	}

	private void clientAdded(ClientData cd) {

		if (clients.contains(cd)) {
			ClientData c = clients.get(clients.indexOf(cd));
			if (c.hasRouteToOther == cd.hasRouteToOther
					&& c.hasDefaultRoute == cd.hasDefaultRoute
					&& c.linkQuality == cd.linkQuality
					&& c.neighborLinkQuality == cd.neighborLinkQuality) {
				return; // no change
			}
			synchronized (clients) {
				clients.remove(c);
				clients.add(cd);
			}
			update();
			return;
		}
		synchronized (clients) {
			clients.add(cd);
		}
		app.clientAdded(cd);
	}

}

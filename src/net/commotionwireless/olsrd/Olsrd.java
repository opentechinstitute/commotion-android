package net.commotionwireless.olsrd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.os.Handler;
import android.util.Log;


public class Olsrd {
	static {
		//System.loadLibrary("servald");
		System.load("/data/data/net.commotionwireless.meshtether/app_bin/libjniolsrd.so");
	}

	public InputStream mInputStream = null, mErrorStream = null;
	public boolean mStreamsAvailable = false;
	public Object mLocker = null;

	private Thread mOlsrdThread, mErrorIoThread, mOutputIoThread;
	private Handler mHandler;

	public native int main(String[] args);

	public Olsrd(Handler handler) {
		mLocker = new Object();
		mHandler = handler;
	}
	public void stopMain() {
		mOlsrdThread.interrupt();
		mErrorIoThread.interrupt();
		mOutputIoThread.interrupt();
	}
	public void startMain(final String[] args) {
		mOlsrdThread = new Thread() {
			public void run() {
				Thread.currentThread().setName("olsrd");
				main(args);
			}
		};
		mOlsrdThread.start();

		Log.d("Olsrd", "Waiting for streams to become available.");
		synchronized (mLocker) {
			while (!mStreamsAvailable) {
				try {
					mLocker.wait();
				} catch (InterruptedException e)
				{
				}
			}
		}
		Log.d("Olsrd", "Streams are available.");

		if (mInputStream == null || mErrorStream == null) {
			Log.e("Olsrd", "Streams not initialized!");
		}
		mErrorIoThread = new Thread() {
			public void run() {
				BufferedReader mBre = new BufferedReader(new InputStreamReader(mErrorStream), 4096);

				Thread.currentThread().setName("olsrd/error_io");
				try{
					String line;
					do {
						line = mBre.readLine();
						mHandler.dispatchMessage(mHandler.obtainMessage(0, line));
					} while (line != null);
				} catch (Exception e) {
					/*
					 * don't worry too much.
					 */
					Log.i("Olsrd", "Exception reading process output: " + e.toString());
				}
				try {
					mBre.close();
				} catch (IOException e) {
					/*
					 * don't really care.
					 */
				}
			}
		};
		mErrorIoThread.start();
		mOutputIoThread = new Thread() {
			public void run() {
				BufferedReader mBr = new BufferedReader(new InputStreamReader(mInputStream), 4096);

				Thread.currentThread().setName("olsrd/output_io");
				try{
					String line;
					do {
						line = mBr.readLine();
						mHandler.dispatchMessage(mHandler.obtainMessage(0, line));
					} while (line != null);
				} catch (Exception e) {
					/*
					 * don't worry too much.
					 */
					Log.i("Olsrd", "Exception reading process output: " + e.toString());
				}
				try {
					mBr.close();
				} catch (IOException e) {
					/*
					 * don't really care.
					 */
				}
			}
		};
		mOutputIoThread.start();
	}
}
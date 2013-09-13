package net.commotionwireless.meshtether;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class MeshTetherProcess {
	private final static int INPUT_THREAD = 0;
	private final static int ERROR_THREAD = 1;

	private boolean mRunning;
	private Thread mIoThreads[] = new Thread[2];
	private Process mProcess = null;
	private String mProg;
	private String[] mEnvp;
	private File mDirectory;
	private int mExitValue;

	public MeshTetherProcess(String prog, String[] envp, File directory) {
		mProg = prog;
		mEnvp = envp;
		mDirectory = directory;
		mRunning = false;
		mExitValue = 0;
	}

	private void cleanupIoThreads() throws IOException {
		mProcess.getInputStream().close();
		mProcess.getErrorStream().close();

		mIoThreads[INPUT_THREAD].interrupt();
		mIoThreads[ERROR_THREAD].interrupt();

		mIoThreads[INPUT_THREAD] = null;
		mIoThreads[ERROR_THREAD] = null;
	}
	
	public void stop() throws IOException, InterruptedException {
		if (mProcess != null) {
			mProcess.destroy();

			cleanupIoThreads();

			mProcess.waitFor();
			mExitValue = mProcess.exitValue();
			mProcess = null;
			mRunning = false;
		}
	}

	public void run(Handler handler, int outputTag, int errorTag) throws IOException {
		mProcess = Runtime.getRuntime().exec(mProg, mEnvp, mDirectory);
		mIoThreads[INPUT_THREAD] = new Thread(new OutputMonitor(handler, outputTag, mProcess.getInputStream()));
		mIoThreads[ERROR_THREAD] = new Thread(new OutputMonitor(handler, errorTag, mProcess.getErrorStream()));
		mIoThreads[INPUT_THREAD].start();
		mIoThreads[ERROR_THREAD].start();
		mRunning = true;
	}

	public void tell(byte[] msg) throws IOException {
		if (mRunning)
			mProcess.getOutputStream().write(msg);
	}
	
	public void runUntilExit(Handler handler, int outputTag, int errorTag) throws IOException, InterruptedException {
		run(handler, outputTag,errorTag);
		mProcess.waitFor();
		mProcess.destroy();
		
		cleanupIoThreads();

		mExitValue = mProcess.exitValue();

		mProcess = null;
		mRunning = false;
	}
	public int exitValue() {
		if (!mRunning)
			return mExitValue;
		else
			return 0;
	}

	/** Worker Threads */
	private class OutputMonitor implements Runnable {
		private final BufferedReader mBr;
		private Handler mHandler;
		public OutputMonitor(Handler handler, int t, InputStream is) {
			mBr = new BufferedReader(new InputStreamReader(is), 8192);
			mHandler = handler;
		}
		@Override
		public void run() {
			try{
				String line;
				Message msg;
				do {
					line = mBr.readLine();
					if (mHandler != null) {
						msg = mHandler.obtainMessage(0, new String(line));
						mHandler.dispatchMessage(msg);
					}
				} while(line != null);
			} catch (Exception e) {
				/*
				 * don't worry too much.
				 */
				Log.i("MeshTetherProcess", "Exception reading process output: " + e.toString());
			}
		}
	}
}

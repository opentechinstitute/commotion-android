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

	public static class ProcessReader {
		private final InputStream mIs;
		private int mBufferSize = 80;
		private byte mBuffer[] = null;
		private int mBufferOffset = 0;
		private String mHold = new String("");
		private boolean mErrorCondition;

		public ProcessReader(InputStream is, int bufferLen) {
			mIs = is;
			mBufferSize = bufferLen;
			mBuffer = new byte[mBufferSize];
			mBufferOffset = 0;
			bufferLen = 0;
			mErrorCondition = false;
		}

		public ProcessReader(InputStream is) {
			this(is, 80);
		}

		private String extractToEndOfLine() {
			int endOfLine = 0;
			if ((endOfLine = (new String(mBuffer, 0, mBufferOffset)).indexOf('\n')) != -1) {
				return extractSubstring(0, endOfLine + 1);
			}
			return null;
		}

		private String extractSubstring(int beginning, int length) {
			String substring;
			byte tBuffer[] = new byte[mBufferSize];

			System.arraycopy(mBuffer, beginning + length, tBuffer, 0, mBufferSize - (beginning + length));
			substring = new String(mBuffer, beginning, length);
			mBuffer = tBuffer;
			mBufferOffset -= (beginning + length);

			return substring;
		}

		private String cleanupExcess() {
			String line = null;
			/*
			 * Grab what's left in the buffer.
			 */
			line = mHold = mHold + extractSubstring(0, mBufferOffset);
			if (mHold.length() == 0) return null;
			mHold = null;
			return line;
		}

		public String readLine() {
			if (mErrorCondition) return null;
			do {
				try {
					int justRead = 0;
					String lineInBuffer = null;

					/*
					 * See if there is a line already in the input!
					 */
					if ((lineInBuffer = extractToEndOfLine()) != null) {
						String line = mHold + lineInBuffer;
						mHold = new String("");
						return line;
					}

					if ((justRead = mIs.read(mBuffer, mBufferOffset, mBufferSize - mBufferOffset)) <= 0) {
						return cleanupExcess();
					}
					mBufferOffset += justRead;

					/*
					 * See if this read fills out our buffer!
					 */
					if (mBufferOffset == mBufferSize) {
						mHold = mHold + extractSubstring(0, mBufferOffset);
					}
				} catch (IOException e) {
					mErrorCondition = true;
					return cleanupExcess();
				}
			} while (true);
		}
	}

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
		private final InputStream mIs;
		private Handler mHandler;
		private MeshTetherProcess.ProcessReader mPr;
		public OutputMonitor(Handler handler, int t, InputStream is) {
			mIs = is;
			mBr = new BufferedReader(new InputStreamReader(is), 160);
			mHandler = handler;
			mPr = new ProcessReader(is);
		}
		@Override
		public void run() {
			try{
				String line;
				Message msg;
				do {
					line = mPr.readLine();
					if (mHandler != null) {
						msg = mHandler.obtainMessage(0, new String(line));
						mHandler.dispatchMessage(msg);
					}
				} while (line != null);
			} catch (Exception e) {
				/*
				 * don't worry too much.
				 */
				Log.i("MeshTetherProcess", "Exception reading process output: " + e.toString());
			}
		}
	}
}

package net.commotionwireless.meshtether;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

public class NativeHelper {
	public static final String TAG = "NativeHelper";

	public static File publicFiles;
	public static File profileDir;
	public static File app_bin;
	public static File app_log;

	static String SU_C;
	static String RUN;
	static String STOP_OLSRD;
	static String DO_STOP_OLSRD;
	static String DEL_ROUTE;
	static String OLSRD;
	static String WIFI;
	static String BUSYBOX;

	public static void setup(Context context) {
		app_bin = context.getDir("bin", Context.MODE_PRIVATE).getAbsoluteFile();
		app_log = context.getDir("log", Context.MODE_PRIVATE).getAbsoluteFile();
		// this is the same as android-8's getExternalFilesDir() but works on android-1
		publicFiles = new File(Environment.getExternalStorageDirectory(),
				"Android/data/" + context.getPackageName() + "/files/");
		publicFiles.mkdirs();
		profileDir = new File(Environment.getExternalStorageDirectory(), "MeshTether");
		profileDir.mkdirs();
		SU_C = new File(app_bin, "su_c").getAbsolutePath();
		STOP_OLSRD = new File(app_bin, "stop_olsrd").getAbsolutePath();
		DO_STOP_OLSRD = new File(app_bin, "do_stop_olsrd").getAbsolutePath();
		DEL_ROUTE = new File(app_bin, "del-fake-default-route").getAbsolutePath();
		RUN = new File(app_bin, "run").getAbsolutePath();
		OLSRD = new File(app_bin, "olsrd").getAbsolutePath();
		WIFI = new File(app_bin, "wifi").getAbsolutePath();
		BUSYBOX = new File(app_bin, "busybox").getAbsolutePath();
	}

	public static boolean unzipAssets(Context context) {
		boolean result = true;
		try {
			AssetManager am = context.getAssets();
			final String[] assetList = am.list("");

			// ignore folder added to the assets on various devices
			for (String asset : assetList) {
				if (asset.equals("images")
						|| asset.equals("sounds")
						|| asset.equals("webkit")
						|| asset.equals("databases")  // Motorola
						|| asset.equals("kioskmode")) // Samsung
					continue;

				int BUFFER = 2048;
				final File file = new File(NativeHelper.app_bin, asset);
				InputStream tmp;
				try {
					tmp = am.open(asset);
				} catch (FileNotFoundException e) {
					// if asset is a directory, we'll get this exception
					e.printStackTrace();
					continue;
				}
				final InputStream assetIS = tmp;

				if (file.exists()) {
					file.delete();
					Log.i(MeshTetherApp.TAG, "DebiHelper.unzipDebiFiles() deleting "
							+ file.getAbsolutePath());
				}

				FileOutputStream fos = new FileOutputStream(file);
				BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);

				int count;
				byte[] data = new byte[BUFFER];

				while ((count = assetIS.read(data, 0, BUFFER)) != -1) {
					dest.write(data, 0, count);
				}

				dest.flush();
				dest.close();

				assetIS.close();
			}
		} catch (IOException e) {
			result = false;
			Log.e(MeshTetherApp.TAG, "Can't unzip", e);
		}
		chmod("0750", new File(SU_C));
		chmod("0750", new File(RUN));
		chmod("0750", new File(STOP_OLSRD));
		chmod("0750", new File(DO_STOP_OLSRD));
		chmod("0750", new File(DEL_ROUTE));
		chmod("0750", new File(OLSRD));
		chmod("0750", new File(WIFI));
		chmod("0750", new File(BUSYBOX));
		chmod("0750", new File(app_bin, "script_aria"));
		chmod("0750", new File(app_bin, "script_hero"));
		chmod("0750", new File(app_bin, "script_samsung"));
		return result;
	}

	public static boolean installBusyboxSymlinks() {
		File testFile = new File(NativeHelper.app_bin, "awk");
		if (testFile.exists()) {
			Log.v(TAG, "busybox test file exists: " + testFile);
		} else {
			// setup busybox so we have the utils we need, guaranteed
			String command = new File(NativeHelper.app_bin, "busybox").getAbsolutePath()
					+ " --install -s " + NativeHelper.app_bin.getAbsolutePath();
			Log.i(TAG, "Running " + command);
			try {
				Process sh = Runtime.getRuntime().exec(command);
				sh.waitFor();
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	public static void chmod(String modestr, File path) {
		Log.i(TAG, "chmod " + modestr + " " + path.getAbsolutePath());
		try {
			Class<?> fileUtils = Class.forName("android.os.FileUtils");
			Method setPermissions = fileUtils.getMethod("setPermissions", String.class,
					int.class, int.class, int.class);
			int mode = Integer.parseInt(modestr, 8);
			int a = (Integer) setPermissions.invoke(null, path.getAbsolutePath(), mode,
					-1, -1);
			if (a != 0) {
				Log.i(TAG, "ERROR: android.os.FileUtils.setPermissions() returned " + a
						+ " for '" + path + "'");
			}
		} catch (ClassNotFoundException e) {
			Log.i(TAG, "android.os.FileUtils.setPermissions() failed:", e);
		} catch (IllegalAccessException e) {
			Log.i(TAG, "android.os.FileUtils.setPermissions() failed:", e);
		} catch (InvocationTargetException e) {
			Log.i(TAG, "android.os.FileUtils.setPermissions() failed:", e);
		} catch (NoSuchMethodException e) {
			Log.i(TAG, "android.os.FileUtils.setPermissions() failed:", e);
		}
	}

	public static boolean isSdCardPresent() {
		return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
	}

	public static void zip(File fileToZip, File zip)
	throws FileNotFoundException, IOException {
		final int BUFFER = 2048;
		BufferedInputStream origin = null;
		byte data[] = new byte[BUFFER];

		FileInputStream fi = new FileInputStream(fileToZip);
		origin = new BufferedInputStream(fi, BUFFER);

		ZipOutputStream out;
		out = new ZipOutputStream(new FileOutputStream(zip));
		ZipEntry e = new ZipEntry(fileToZip.getName());
		out.putNextEntry(e);
		int count;
		while ((count = origin.read(data, 0, BUFFER)) != -1)
			out.write(data, 0, count);
		out.close();
	}
}

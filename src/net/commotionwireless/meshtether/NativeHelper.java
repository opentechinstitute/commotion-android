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
import java.util.HashMap;
import java.util.Map;
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
	public static File shared_prefs;

	public static String SU_C;
	private static String RUN_OLSRD;
	private static String OLSRD;
	private static String STOP_OLSRD;
	private static String DO_STOP_OLSRD;
	private static String SERVALD;
	private static String DEFAULT_PROFILE;


	public static void setup(Context context) {
		app_bin = context.getDir("bin", Context.MODE_PRIVATE).getAbsoluteFile();
		app_log = context.getDir("log", Context.MODE_PRIVATE).getAbsoluteFile();
		shared_prefs = new File(context.getFilesDir().getParent() + "/shared_prefs" + "/");

		// this is the same as android-8's getExternalFilesDir() but works on android-1
		publicFiles = new File(Environment.getExternalStorageDirectory(),
				"Android/data/" + context.getPackageName() + "/files/");
		publicFiles.mkdirs();
		profileDir = new File(Environment.getExternalStorageDirectory(), "MeshTether");
		profileDir.mkdirs();
		SU_C = new File(app_bin, "su_c").getAbsolutePath();
		RUN_OLSRD = new File(app_bin, "run_olsrd").getAbsolutePath();
		OLSRD = new File(app_bin, "olsrd").getAbsolutePath();
		STOP_OLSRD = new File(app_bin, "stop_olsrd").getAbsolutePath();
		DO_STOP_OLSRD = new File(app_bin, "do_stop_olsrd").getAbsolutePath();
		SERVALD = new File(app_bin, "servald").getAbsolutePath();
		DEFAULT_PROFILE = new File(shared_prefs, "commotionwireless.net.xml").getAbsolutePath();
	}

	public static boolean unzipAssets(Context context) {
		boolean result = true;
		try {
			AssetManager am = context.getAssets();
			final String[] assetList = am.list("");
			Map<String,File> unzipTo = new HashMap<String,File>();
			Map<String,Boolean> overwriteOnUnzip = new HashMap<String,Boolean>();

			/*
			 * setup unzip location for .xml files and
			 * signal that we don't want to overwrite a
			 * file if it already exists.
			 */
			unzipTo.put(".xml", NativeHelper.shared_prefs);
			overwriteOnUnzip.put(".xml", Boolean.valueOf(false));

			// ignore folder added to the assets on various devices
			for (String asset : assetList) {
				if (asset.equals("images")
						|| asset.equals("sounds")
						|| asset.equals("webkit")
						|| asset.equals("databases")  // Motorola
						|| asset.equals("kioskmode")) // Samsung
					continue;
				int BUFFER = 2048;
				final File file;
				String extension = asset.substring((asset.lastIndexOf('.') != -1) ? asset.lastIndexOf('.') : 0);
				File directory = unzipTo.get(extension);
				boolean overwrite = (overwriteOnUnzip.get(extension) == null) ? true : overwriteOnUnzip.get(extension);; 

				Log.d("NativeHelper", "extension: " + extension);
				Log.d("NativeHelper", "directory: " +
						((directory != null) ? directory.getAbsolutePath() : "N/A") +
						((overwrite) ? "(overwrite)" : "(keep)"));
				if (directory != null) {
					file = new File(directory, asset);
					if (file.exists() && !overwrite) {
						continue;
					}
				} else {
					file = new File(NativeHelper.app_bin, asset);
				}

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
		chmod("0750", new File(RUN_OLSRD));
		chmod("0750", new File(OLSRD));
		chmod("0750", new File(STOP_OLSRD));
		chmod("0750", new File(DO_STOP_OLSRD));
		chmod("0750", new File(SERVALD));
		chmod("0660", new File(DEFAULT_PROFILE));

		return result;
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
		out.closeEntry();
		origin.close();
		out.close();
	}
}

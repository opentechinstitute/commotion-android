/**
 * Copyright (C) 2011 The Serval Project
 *
 * This file is part of Serval Software (http://www.servalproject.org)
 *
 * Serval Software is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.servalproject.servald;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/*
import org.servalproject.ServalBatPhoneApplication;
import org.servalproject.rhizome.RhizomeManifest;
import org.servalproject.rhizome.RhizomeManifestParseException;
*/

import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * Low-level class for invoking servald JNI command-line operations.
 *
 * @author Andrew Bettison <andrew@servalproject.com>
 */
public class ServalD
{

	public static final String TAG = "ServalD";
	private static long started = -1;
	static boolean log = false;
	public static String ServalInstancePath = "/var/serval-node";
	
	private ServalD() {
	}

	static {
		//System.loadLibrary("servald");
		System.load("/data/data/net.commotionwireless.meshtether/app_bin/libservald.so");
	}

	/**
	 * Low-level JNI entry point into servald command line.
	 *
	 * @param outv	A list to which the output fields will be appended using add()
	 * @param args	The words to pass on the command line (ie, argv[1]...argv[n])
	 * @return		The servald exit status code (normally 0 indicates success)
	 */
	private static native int rawCommand(IJniResults outv, String[] args)
			throws ServalDInterfaceError;

	/**
	 * Common entry point into servald command line.
	 *
	 * @param callback
	 *            Each result will be passed to callback.result(String)
	 *            immediately.
	 * @param args
	 *            The parameters as passed on the command line, eg: res =
	 *            servald.command("config", "set", "debug", "peers");
	 * @return The servald exit status code (normally0 indicates success)
	 */
	private static synchronized int command(final IJniResults callback, String... args)
			throws ServalDInterfaceError
	{
		String aArgs[] = null;
		int counter = 0;
		
		aArgs = new String[args.length + 1];
		
		counter = 0;
		for (String a : args) {
			aArgs[counter+1] = a;
			counter++;
		}
		
		aArgs[0] = ServalD.ServalInstancePath;
		
		if (log)
			Log.i(ServalD.TAG, "args = " + Arrays.deepToString(args));
		
		return rawCommand(callback, aArgs);
	}

	/**
	 * Common entry point into servald command line.
	 *
	 * @param args
	 *            The parameters as passed on the command line, eg: res =
	 *            servald.command("config", "set", "debug", "peers");
	 * @return An object containing the servald exit status code (normally0
	 *         indicates success) and zero or more output fields that it would
	 *         have sent to standard output if invoked via a shell command line.
	 */

	private static synchronized ServalDResult command(String... args)
			throws ServalDInterfaceError
	{
		String aArgs[] = null;
		int counter = 0;
		
		aArgs = new String[args.length + 1];
		
		counter = 0;
		for (String a : args) {
			aArgs[counter+1] = a;
			counter++;
		}
		
		aArgs[0] = ServalD.ServalInstancePath;
		
		if (log)
			Log.i(ServalD.TAG, "args = " + Arrays.deepToString(args));
		LinkedList<byte[]> outv = new LinkedList<byte[]>();
		int status = rawCommand(new JniResultsList(outv), aArgs);
		if (log) {
			LinkedList<String> outvstr = new LinkedList<String>();
			for (byte[] a: outv)
				outvstr.add(a == null ? null : new String(a));
			Log.i(ServalD.TAG, "result = " + Arrays.deepToString(outvstr.toArray()));
			Log.i(ServalD.TAG, "status = " + status);
		}
		return new ServalDResult(args, status, outv.toArray(new byte[outv.size()][]));
	}

	/** Start the servald server process if it is not already running.
	 *
	 * @author Andrew Bettison <andrew@servalproject.com>
	 */
	public static void serverStart(String execPath)
			throws ServalDFailureException, ServalDInterfaceError {
		ServalDResult result = command("start", "exec", execPath);
		result.failIfStatusError();
		started = System.currentTimeMillis();
		Log.i(ServalD.TAG, "server " + (result.status == 0 ? "started" : "already running") + ", pid=" + result.getFieldInt("pid"));
	}

	/*
	public static void serverStart() throws ServalDFailureException,
			ServalDInterfaceError {
		serverStart(ServalBatPhoneApplication.context.coretask.DATA_FILE_PATH
				+ "/bin/servald");
	}
	*/
	
	/** Stop the servald server process if it is running.
	 *
	 * @author Andrew Bettison <andrew@servalproject.com>
	 */
	public static void serverStop() throws ServalDFailureException,
			ServalDInterfaceError {
		ServalDResult result = command("stop");
		started = -1;
		result.failIfStatusError();
		Log.i(ServalD.TAG, "server " + (result.status == 0 ? "stopped, pid=" + result.getFieldInt("pid") : "not running"));
	}

	/** Query the servald server process status.
	 *
	 * @return	True if the process is running
	 * @author Andrew Bettison <andrew@servalproject.com>
	 */
	public static boolean serverIsRunning() throws ServalDFailureException, ServalDInterfaceError {
		ServalDResult result = command("status");
		result.failIfStatusError();
		return result.status == 0;
	}

	public static long uptime() {
		if (started == -1)
			return -1;
		return System.currentTimeMillis() - started;
	}

	/** The result of a lookup operation.
	 *
	 * @author Andrew Bettison <andrew@servalproject.com>
	 */
	protected static class LookupResult extends ServalDResult {
		public final SubscriberId subscriberId;
		public final String did;
		public final String name;
		/** Copy constructor. */
		protected LookupResult(LookupResult orig) {
			super(orig);
			this.subscriberId = orig.subscriberId;
			this.did = orig.did;
			this.name = orig.name;
		}
		/** Unpack a result from a keyring add operation.
		*
		* @param result		The result object returned by the operation.
		*
		* @author Andrew Bettison <andrew@servalproject.com>
		*/
		protected LookupResult(ServalDResult result) throws ServalDInterfaceError {
			super(result);
			if (result.status == 0) {
				this.subscriberId = getFieldSubscriberId("sid");
				this.did = getFieldStringNonEmptyOrNull("did");
				this.name = getFieldStringNonEmptyOrNull("name");
			} else {
				this.subscriberId = null;
				this.did = null;
				this.name = null;
			}
		}
	}

	/** The result of a keyring add operation.
	 *
	 * @author Andrew Bettison <andrew@servalproject.com>
	 */
	protected static class KeyringAddResult extends LookupResult {
		/** Copy constructor. */
		protected KeyringAddResult(KeyringAddResult orig) {
			super(orig);
		}
		/** Unpack a result from a keyring add operation.
		*
		* @param result		The result object returned by the operation.
		*
		* @author Andrew Bettison <andrew@servalproject.com>
		*/
		protected KeyringAddResult(ServalDResult result) throws ServalDInterfaceError {
			super(result);
		}
	}

	public static KeyringAddResult keyringAdd() throws ServalDFailureException, ServalDInterfaceError
	{
		ServalDResult result = command("keyring", "add");
		result.failIfStatusError();
		return new KeyringAddResult(result);
	}

	public static KeyringAddResult keyringSetDidName(SubscriberId sid, String did, String name) throws ServalDFailureException, ServalDInterfaceError
	{
		List<String> args = new LinkedList<String>();
		args.add("keyring");
		args.add("set");
		args.add("did");
		args.add(sid.toHex().toUpperCase());
		if (did != null)
			args.add(did);
		else if (name != null)
			args.add("");
		if (name != null)
			args.add(name);
		ServalDResult result = command(args.toArray(new String[args.size()]));
		result.failIfStatusError();
		return new KeyringAddResult(result);
	}

	/** The result of a keyring list.
	 *
	 * @author Andrew Bettison <andrew@servalproject.com>
	 */
	public static class KeyringListResult extends ServalDResult {
		public static class Entry {
			public final SubscriberId subscriberId;
			public final String did;
			public final String name;
			protected Entry(SubscriberId sid, String did, String name) {
				this.subscriberId = sid;
				this.did = did;
				this.name = name;
			}
		}
		public final Entry[] entries;
		/** Copy constructor. */
		protected KeyringListResult(KeyringListResult orig) {
			super(orig);
			this.entries = orig.entries;
		}
		/** Unpack a result from a keyring list output.
		*
		* @param result		The result object returned by the operation.
		*
		* @author Andrew Bettison <andrew@servalproject.com>
		*/
		protected KeyringListResult(ServalDResult result) throws ServalDInterfaceError {
			super(result);
			if (this.outv.length % 3 != 0)
				throw new ServalDInterfaceError("invalid number of fields " + this.outv.length + " (not multiple of 3)", this);
			Entry[] entries = new Entry[this.outv.length / 3];
			for (int i = 0; i != this.outv.length; i += 3)
				try {
					entries[i / 3] = new Entry(
							new SubscriberId(new String(this.outv[i])),
							this.outv[i + 1].length != 0 ? new String(this.outv[i + 1]) : null,
							this.outv[i + 2].length != 0 ? new String(this.outv[i + 2]) : null
						);
				} catch (SubscriberId.InvalidHexException e) {
					throw new ServalDInterfaceError("invalid output field outv[" + i + "]", this, e);
				}
			this.entries = entries;
		}
	}

	public static KeyringListResult keyringList() throws ServalDFailureException, ServalDInterfaceError
	{
		ServalDResult result = command("keyring", "list");
		result.failIfStatusError();
		return new KeyringListResult(result);
	}
	public static int getPeerCount() throws ServalDFailureException {
		ServalDResult result = ServalD.command("peer", "count");
		result.failIfStatusError();
		return Integer.parseInt(new String(result.outv[0]));
	}

	public static int peers(final IJniResults callback) throws ServalDInterfaceError
	{
		return command(callback, "id", "peers");
	}

	public static LookupResult reverseLookup(SubscriberId sid) throws ServalDFailureException, ServalDInterfaceError
	{
		ServalDResult result = ServalD.command("reverse", "lookup", sid.toHex().toUpperCase());
		result.failIfStatusError();
		return new LookupResult(result);
	}

	// MeshMS API
	public static Cursor listConversations(final SubscriberId sender)
			throws ServalDFailureException, ServalDInterfaceError
	{
		return new ServalDCursor() {
			@Override
			void fillWindow(CursorWindowJniResults window, int offset, int numRows) throws ServalDFailureException {
				int ret = ServalD.command(window, "meshms", "list", "conversations",
						sender.toHex().toUpperCase(), ""+offset, ""+numRows);
				if (ret!=0)
					throw new ServalDFailureException("Exit code "+ret);
			}
		};
	}

	public static Cursor listMessages(final SubscriberId sender, final SubscriberId recipient)
			throws ServalDFailureException, ServalDInterfaceError
	{
		return new ServalDCursor() {
			@Override
			void fillWindow(CursorWindowJniResults window, int offset, int numRows) throws ServalDFailureException {
				if (offset!=0 || numRows!=-1)
					throw new ServalDFailureException("Only one window supported");
				Log.v(TAG, "running meshms list messages "+sender+", "+recipient);
				int ret = ServalD.command(window, "meshms", "list", "messages",
						sender.toHex().toUpperCase(), recipient.toHex().toUpperCase());
				if (ret!=0)
					throw new ServalDFailureException("Exit code "+ret);
			}
		};
	}

	public static void sendMessage(final SubscriberId sender, final SubscriberId recipient, String message) throws ServalDFailureException {
		ServalDResult ret = ServalD.command("meshms", "send", "message",
				sender.toHex().toUpperCase(), recipient.toHex().toUpperCase(),
				message);
		ret.failIfStatusNonzero();
	}

	public static void readMessage(final SubscriberId sender, final SubscriberId recipient) throws ServalDFailureException {
		ServalDResult ret = ServalD.command("meshms", "read", "messages",
				sender.toHex().toUpperCase(), recipient.toHex().toUpperCase());
		ret.failIfStatusNonzero();
	}

	public static void readMessage(final SubscriberId sender, final SubscriberId recipient, long offset) throws ServalDFailureException {
		ServalDResult ret = ServalD.command("meshms", "read", "messages",
				sender.toHex().toUpperCase(), recipient.toHex().toUpperCase(),
				""+offset);
		ret.failIfStatusNonzero();
	}
}

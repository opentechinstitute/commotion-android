package net.commotionwireless.olsrd;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class DotConf {
	private String mBaseConfiguration;
	private Vector<DotConf.Stanza> mStanzas;
	
	public DotConf(String baseConfiguration) throws IOException, FileNotFoundException {
		this(new FileInputStream(baseConfiguration));
	}
	
	public DotConf(InputStream base) throws IOException {
		StringBuilder builder = new StringBuilder();
		BufferedReader buf = new BufferedReader(new InputStreamReader(base));		
		if (buf != null) {
			String line;
			while ((line = buf.readLine()) != null) {
				builder.append(line);
				builder.append("\n");
			}
			buf.close();
		}
		mBaseConfiguration = new String(builder);
		mStanzas = new Vector<DotConf.Stanza>();
	}
	
	public void addStanza(DotConf.Stanza newStanza) {
		mStanzas.add(newStanza);
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder(mBaseConfiguration);
		for (DotConf.Stanza s : mStanzas) {
			builder.append(s.toString());
		}
		return new String(builder);
	}
	
	public void write(OutputStream out) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
		if (writer != null) {
			writer.write(toString());
			writer.flush();
			writer.close();
		}
	}
	
	public static class Stanza {
		protected Map<String, String> mKvs;
		protected String mDirective;
		protected String mDirectiveParameter;
		public Stanza() {
			mKvs = new HashMap<String,String>();
		}
		public void addKeyValue(String key, String value) {
			mKvs.put(key, value);
		}
		public String toString() {
			StringBuilder builder = new StringBuilder();
			if (mDirective != null && mDirectiveParameter != null) {
				builder.append(mDirective);
				builder.append(" ");
				builder.append(mDirectiveParameter);
			}
			builder.append("{\n");
			for (Map.Entry<String, String> e : mKvs.entrySet()) {
				builder.append("\"" + e.getKey() + "\"=\"" + e.getValue() + "\"\n");
			}
			builder.append("}\n");
			return new String(builder);
		}
	}
	public static class PluginStanza extends Stanza {
		public PluginStanza(String pluginPath) {
			super();
			mDirective = "LoadPlugin";
			mDirectiveParameter = pluginPath;
		}
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(mDirective);
			builder.append(" ");
			builder.append(mDirectiveParameter);
			builder.append("{\n");
			for (Map.Entry<String, String> e : mKvs.entrySet()) {
				builder.append("\tPlParam \"" + e.getKey() + "\"=\"" + e.getValue() + "\"\n");
			}
			builder.append("}\n");
			return new String(builder);
		}
	}
}

package net.commotionwireless.olsrd;


public class Olsrd {
	static {
		//System.loadLibrary("servald");
		System.load("/data/data/net.commotionwireless.meshtether/app_bin/libolsrd.so");
	}
	public native int main(String[] args);
}
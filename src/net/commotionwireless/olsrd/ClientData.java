package net.commotionwireless.olsrd;

public class ClientData {
	private static int MAXIMUM_MISSED_UPDATES = 3;

	public final String remoteIP;
	public final float linkQuality;
	public final float neighborLinkQuality;
	public final int linkCost;
	public final int validityTime;
	public boolean hasRouteToOther;
	public boolean hasDefaultRoute;
	public int missedUpdateCounter;
	public ClientData(String ip, float lq, float nlq, int lc, int vt) {
		remoteIP = ip;
		linkQuality = lq;
		neighborLinkQuality = nlq;
		linkCost = lc;
		validityTime = vt;
		hasRouteToOther = false;
		hasDefaultRoute = false;
		missedUpdateCounter = 0;
	}
	@Override
	public String toString() { return remoteIP + " " + linkQuality + " " + neighborLinkQuality; }
	public String toNiceString() { return remoteIP; }
	public boolean equals(Object b) {
		if (b instanceof ClientData) {
			ClientData bb = (ClientData)b;
			if (remoteIP.equalsIgnoreCase(bb.remoteIP))
				return true;
		}
		return false;
	}
	public int hashCode() {
		return remoteIP.hashCode();
	}
	public boolean shouldRemove() {
		return (missedUpdateCounter>MAXIMUM_MISSED_UPDATES);
	}
}

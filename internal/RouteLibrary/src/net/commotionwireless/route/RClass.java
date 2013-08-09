package net.commotionwireless.route;

/**
 * The base class for all reflected classes. Provides 
 * - equals()
 * - hashCode()
 * - toString()
 * - getNativeClass()
 * - getNativeObject()
 * for all subclasses. Those who extend this class
 * must set mNativeClass and mNativeObject in the
 * constructor for this to work correctly.
 * 
 * @author Will Hawkins
 * @version 0.01
 */
public class RClass {
	
	protected Class mNativeClass;
	protected Object mNativeObject;
	
	public Class getNativeClass() {
		return mNativeClass;
	}
	
	public Object getNativeObject() {
		return mNativeObject;
	}
	
	public String toString() {
		return mNativeObject.toString();
	}
	
	/**
	 * Use the native object's equals() method
	 * to determine if two reflected objects
	 * are actually equal.
	 * 
	 * @param other The object against which to compare.
	 * @return true/false depending on whether {@code this}
	 * is the same as {@code other}.
	 */
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof RClass)) return false;
		return mNativeObject.equals(((RClass)other).getNativeObject());
	}
	
	@Override 
	public int hashCode() {
		return mNativeObject.hashCode();
	}
}

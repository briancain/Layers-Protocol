/**
 * This class contains various constants.
 * @author sgujrati
 *
 */
public final class Common {
	/* 
	 * You  may use headers at various layers to send 
	 * various informations, such as time stamps. 
	 * The headers at each layer are of type char[], so you 
	 * may want to write utility functions to convert char[]
	 * to int or double or float and vice versa.
	 */
	public static final int rlHeaderLen = 10;
	public static final int dllHeaderLen = 10;
	public static final int phyHeaderLen = 10;
	public static final int queueCapacity = 20;
	public static final int MinError = 0;
	public static final int MaxError = 100;
	// Set the error rate here between 0 to 99
	//public static final int ErrorRate = 10;
	//public static final int ErrorRate = 20;
	public static final int ErrorRate = 30;
	//public static final int ErrorRate = 40;
}


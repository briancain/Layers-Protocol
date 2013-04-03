import java.io.Serializable;

/**
 * Dll layer packet. It adds DllHeader to RlPacket
 * @author sgujrati
 *
 */
public class DllPacket implements Serializable{
	private static final long serialVersionUID = 1L;
	private RlPacket rp;
	private char[] DllHeader;
	DllPacket(RlPacket rp, char[] h) {
		this.rp = new RlPacket(rp);
		DllHeader = h;
	}	
	DllPacket(DllPacket dp){
		this.rp = dp.rp;
		this.DllHeader = dp.DllHeader;
	}	
	public String getHeader(){
		return new String(DllHeader);
	}
	public RlPacket getRlPacket(){
		return rp;
	}
}
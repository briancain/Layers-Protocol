import java.io.Serializable;

/**
 * Routing layer packet. It adds RlHeader to AppPacket
 * @author sgujrati
 *
 */
public class RlPacket implements Serializable{
	private static final long serialVersionUID = 1L;
	private AppPacket ap;
	private char[] RlHeader;
	RlPacket(AppPacket ap, char[] h) {
		this.ap = new AppPacket(ap);
		RlHeader = h;
	}	
	RlPacket(RlPacket rp){
		this.ap = rp.ap;
		this.RlHeader = rp.RlHeader;
	}
	public String getHeader(){
		return new String(RlHeader);
	}
	public AppPacket getAppPacket(){
		return ap;
	}
}
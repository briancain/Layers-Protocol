import java.io.Serializable;

/**
 * Phy layer packet. It adds PhyHeader to DllPacket
 * @author sgujrati
 *
 */
public class PhyPacket implements Serializable{
	private static final long serialVersionUID = 1L;
	private DllPacket dp;
	private char[] PhyHeader;
	PhyPacket(DllPacket dp, char[] h) {
		this.dp = new DllPacket(dp);
		PhyHeader = h;
	}	
	public String getHeader(){
		return new String(PhyHeader);
	}
	public DllPacket getDllPacket(){
		return dp;
	}
}


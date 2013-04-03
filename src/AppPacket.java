import java.io.Serializable;

/**
 * Application layer packet. 
 * @author sgujrati
 *
 */
public class AppPacket implements Serializable{
	private static final long serialVersionUID = 1L;
	private String src;
	private String dest;
	private String payload;
	AppPacket(AppPacket ap){
		this.src = ap.src;
		this.dest = ap.dest;
		this.payload = ap.payload;
	}
	AppPacket(String src, String dest, String payload) {
		this.src = src;
		this.dest = dest;
		this.payload = payload;	
	}	
	public String getSrc(){
		return src;
	}
	public String getDst(){
		return dest;
	}
	public String getPayload(){
		return payload;
	}
}




import java.io.Serializable;

/**
 * A packet consisting of a single String variable. 
 * @author sgujrati
 *
 */
public class StringPacket implements Serializable{
	private static final long serialVersionUID = 1L;
	String s;
	StringPacket(String s){
		this.s = s;
	}
	public String getString(){
		return s;
	}
}
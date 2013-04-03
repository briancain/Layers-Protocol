public class RunServer{
	final RunServer serverinstance = new RunServer();
	
	public static void main(String args[]){
		
		new PhyServer();
		PhyServer.main(new String[]{"topology.txt", "1337"});
	}
}

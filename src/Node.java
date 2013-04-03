/**
 * @author sgujrati
 * This program implements a process running on a node. This process is implemented
 * as a stack of four layers: App <-> Routing <-> DLL <-> Phy. Phy has been implemented.
 */
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.BufferedReader;

/**
 * This class implements the application layer. 
 * @author sgujrati
 *
 */
public class Node{
	// nodeID: a string consisting of only one character
	private String nodeID;
	private Routing rl;
	// This queue stores messages received from RL 
	private SharedQueue<AppPacket> fromRl;

	/**
	 * Default constructor
	 */
	Node(){
	}
	
	/**
	 * This constructor validates arguments and creates a routing layer. 
	 * @param args
	 */
	Node(String[] args){
		if (args.length != 3) {
			System.out
          .println("Usage: java NodeApp <nodeID> <host> <portNumber>\n");
			System.exit(0);
		}
		nodeID = args[0];
		rl = new Routing(args, this);
		fromRl = new SharedQueue<AppPacket>(Common.queueCapacity);
	}
	
	/**
	 * Returns the Node ID of this node
	 * @return
	 */
	public String getNodeID(){
		return nodeID;
	}
	
	/**
	 * Creates a new AppPacket and sends it down to RL. destID = 0
	 * indicates message broadcast. 
	 * @param destID
	 * @param payload
	 */
	public void send(String destID, String payload){
		AppPacket appPkt = new AppPacket(nodeID, destID, payload);
		rl.send(appPkt);
	}
	
	/**
	 * This function, when called by RL inserts an AppPacket to
	 * fromRl queue.
	 * @param appPkt
	 */
	public void receive(AppPacket appPkt){
		fromRl.insert(appPkt);
	}

	/**
	 * This function takes one AppPacket from fromRl and returns it.
	 * @return
	 */
	public AppPacket recv(){
		AppPacket appPkt = fromRl.remove();
		return appPkt;
	}
	
	public static void main(String[] args){
		Node app = new Node(args);
		
		/* Node A opens and transfers a file. */
		if(app.getNodeID().equals("A")){
			BufferedReader br = null;
			String line;
			
			try {
				br = new BufferedReader(new FileReader("temp.txt"));
			} catch (FileNotFoundException e) {
				System.err.println(e);
			}

			try {
				while((line = br.readLine()) != null){ 
					app.send("B", line);
				}
				app.send("B", null);
				br.close();
			} catch (IOException e) {
				System.err.println(e);
			} 
		}
		
		/* 
		 * Node B receives a file from A. Start Node B before starting
		 * Node A
		 */
		if(app.getNodeID().equals("B")){
			BufferedWriter bw = null;
			try {
				bw = new BufferedWriter(new FileWriter("tempB.txt"));
			} catch (IOException e) {
				System.err.println(e);
			}

			try {
				while(true){
					AppPacket appPkt = app.recv();
					//String dest = appPkt.getDst();
					//String src = appPkt.getSrc(); // if src = 0, it means it is a broadcasted message
					//System.out.println(dest + " received \"" + 
					//appPkt.getPayload() + "\" from " + src);
					app.getNodeID();
					
					if(appPkt.getPayload() == null){
						bw.close();
						System.out.println("I received the file. " +
								"I am closing it now and exiting");
						break;
					}
					else
						/* 
						 * Do not forget to append a new line character at
						 * the end of payload if you were reading the file line by line,
						 * and transmitting individual lines. If you are reading the file 
						 * bye by byte, then you may not have to do this.
						 */
						bw.write(appPkt.getPayload()+"\n");
				}
			} catch (IOException e) {
				System.err.println(e);
			}			
		}		
	}
}

/**
 * This class implements routing layer.
 * @author sgujrati
 *
 */
class Routing{
	private Dll dll;	
	private SharedQueue<AppPacket> fromApp;
	private SharedQueue<RlPacket> fromDll;
	private char[] rlHeader;
	
	/**
	 * Default constructor
	 */
	Routing(){		
	}
	
	/**
	 * This constructor creates a DLL and two threads waiting for packets
	 * from app and dll respectively.
	 * @param args
	 * @param app
	 */
	Routing(String[] args, final Node app){	
		dll = new Dll(args, this);
		fromApp = new SharedQueue<AppPacket>(Common.queueCapacity);
		fromDll = new SharedQueue<RlPacket>(Common.queueCapacity);

		/* Waits for packets pushed down by App */
		(new Thread(){
			public void run(){
				while(true){
					AppPacket appPkt = fromApp.remove();
					rlHeader = new char[Common.rlHeaderLen];
					rlHeader[0]='r';rlHeader[1]='l';rlHeader[2]='h';
					RlPacket rlPkt = new RlPacket(appPkt, rlHeader);
					dll.send(rlPkt);				}
			}
		}).start();
		
		/* Waits for packets pushed by Dll */
		(new Thread(){
			public void run(){
				while(true){
					RlPacket rlPkt = fromDll.remove();
					//String header = rlPkt.getHeader();
					//System.out.println("RlHeader: " + header);
					app.receive(rlPkt.getAppPacket());				}
			}
		}).start();
	}	
	
	public void send(AppPacket appPkt){
		fromApp.insert(appPkt);
	}
	
	void receive(RlPacket rlPkt){
		fromDll.insert(rlPkt);
	}	
}

/**
 * This class implements DLL.
 * @author bccain
 *
 */
class Dll{
	private Phy phy;
	private SharedQueue<RlPacket> fromRl;
	private SharedQueue<DllPacket> fromPhy;
	private char[] dllHeader;
	private int num_sent, num_rec, num_ack;
	private final int window = 10; // window size is 10

	/**
	 * Default constructor
	 */
	Dll(){	
	}
	
	/**
	 * This constructor creates a DLL and two threads waiting
	 * for data from rl and phy respectively.
	 * @param args
	 * @param rl
	 */
	Dll(String[] args, final Routing rl){
		phy = new Phy(args, this);
		fromRl = new SharedQueue<RlPacket>(Common.queueCapacity);
		fromPhy = new SharedQueue<DllPacket>(Common.queueCapacity);
		num_sent = 0;
		num_ack = 0;
		num_rec = 0;
		
		/* Waits for packets pushed down by Rl */
		// Physical Layer Send thread
		(new Thread(){
			public void run(){
				while(true){
					RlPacket rlPkt = fromRl.remove();
					dllHeader = new char[Common.dllHeaderLen];
					dllHeader[0]='d';dllHeader[1]='l';dllHeader[2]='l';
					// add ack in header? 10 byte size
					DllPacket dllPkt = new DllPacket(rlPkt, dllHeader);
					phy.send(dllPkt);
				}
			}
		}).start();
		
		/* Waits for packets pushed by Phy */
		// Routing Layer Receive Thread
		(new Thread(){
			public void run(){
				while(true){
					DllPacket dllPkt = fromPhy.remove();
					// Print system header to console
					//String header = dllPkt.getHeader();
					//System.out.println("DllHeader: " + header);
					rl.receive(dllPkt.getRlPacket());
				}
			}
		}).start();
	}
	
	public void send(RlPacket rlPkt){
		fromRl.insert(rlPkt);
	}
	
	public void receive(DllPacket dllPkt){
		fromPhy.insert(dllPkt);
	}	
}

/**
 * This class implements Phy.
 * @author sgujrati
 * @param <PhyPacket>
 *
 */
class Phy{
	// The client socket
	private Socket clientSocket = null;
	// The input stream
	private ObjectInputStream is = null;
	// The output stream
	private ObjectOutputStream os = null;
	private int portNumber;
	private String host;
	private String nodeID;
	// Packet received at physical layer
	private PhyPacket rcvdPkt;
	private Dll dll;
	private char[] phyHeader;

	/**
	 * Default constructor
	 */
	Phy(){
		
	}

	/**
	 * This constructor creates Phy.
	 * @param args
	 * @param dll
	 */
	Phy(String[] args, Dll dll){
		
		this.dll = dll;
		nodeID = args[0];
		host = args[1];
		portNumber = Integer.valueOf(args[2]).intValue();

		/*
		 * Open a socket on a given host and port. 
		 * Open input and output streams.
		 */
		try {
			clientSocket = new Socket(host, portNumber);
			os = new ObjectOutputStream (clientSocket.getOutputStream());
			is = new ObjectInputStream(clientSocket.getInputStream());
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host " + host);
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to the host "
          + host);
		}		
		
		/*  Tell the server your ID */
		StringPacket node = new StringPacket(nodeID);
		try {
			os.writeObject(node);
		} catch (IOException e) {
			System.err.println(e);
		}
		
		/* Wait to receive data from other nodes */
		receive();
	}
	
	/* Sends the packet. */
	public void send(DllPacket dllPkt){
		phyHeader = new char[Common.phyHeaderLen];
		phyHeader[0]='p';phyHeader[1]='h';phyHeader[2]='y';
		PhyPacket phyPkt = new PhyPacket(dllPkt, phyHeader);
		try {
			os.writeObject(phyPkt);
		} catch (IOException e) {
			System.err.println(e);
		}				
	}
	
	public void receive(){		
		(new Thread() {  
			public void run() {					
					try {
						while ((rcvdPkt = (PhyPacket) is.readObject()) != null) {
							//String header = rcvdPkt.getHeader();
							//System.out.println("PhyHeader: " + header);
							dll.receive(rcvdPkt.getDllPacket());
						}
					} catch (IOException e) {
						System.err.println("IOException:  " + e);
					} catch (ClassNotFoundException e){
						System.err.println("IOException:  " + e);
					}
			  }
			 }).start();
	}
}
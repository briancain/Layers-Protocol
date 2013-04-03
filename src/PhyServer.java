/**
 * @author sgujrati
 * This program implements a Sever that maintains topology 
 * and manages communication among various nodes.
 */
import java.io.File;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.BufferedReader;
import java.util.Random;
import java.util.Arrays;

/**
 * implementation of the server
 * @author sgujrati
 *
 */
public class PhyServer{

	// The server socket.
	private static ServerSocket serverSocket = null;
	// The client socket.
	private static Socket nodeSocket = null;
	// Maximum number of nodes in topology
	private static int maxNodesCount;
	private static nodeThread[] threads;
	// Number of extra threads to be created. (We need few extra)
	private static final int extraThreads = 3;
	// total number of threads created = maxNodesCount + extraThreads
	private static int nThreads;
	private static int portNumber;
	private static File topologyFile;
	private static BufferedReader br = null;
	private static String[][] topology;
	
	public static void main(String args[]) {
		if (args.length != 2) {
			System.out.println("Usage: java PhyServer <topologyFile> <portNumber>\n");
			System.exit(0);
		} else {
			topologyFile = new File(args[0]);
			portNumber = Integer.valueOf(args[1]).intValue();
		}

		/* Read topology file and create topology */
		try{			
			br = new BufferedReader(new FileReader(topologyFile));
			
			/* First line of file is number of nodes */
			maxNodesCount = Integer.valueOf(br.readLine().trim()).intValue();
			nThreads = maxNodesCount + extraThreads;
			threads = new nodeThread[nThreads];
			topology = new String[maxNodesCount][maxNodesCount];
			
			/* Read connections and create topology */
			String line;
			for(int i = 0; i <= maxNodesCount-1; i++){
				line = br.readLine().trim();
				String[] connections = line.split(" ");
				for(int j = 0; j < connections.length; j++){
					topology[i][j]=connections[j];	
				}
			}
		}catch(IOException e){
			System.out.println(e);
		}
		
		/* Open a server socket on the portNumber */
		try {
			serverSocket = new ServerSocket(portNumber);
		} catch (IOException e) {
			System.out.println(e);
		}

		/*
		 * Create a node socket for each node and pass it to a new node
		 * thread.
		 */
		while (true) {
			try {
				nodeSocket = serverSocket.accept();
				int i = 0;
				for (i = 0; i < nThreads; i++) {				
					if (threads[i] == null) {
						(threads[i] = new nodeThread(nodeSocket, threads, topology, extraThreads)).start();
						break;
					}
				}
				if (i == nThreads) {
					System.out.println("Server too busy. Try later.");
					nodeSocket.close();
				}
			} catch (IOException e) {
				System.err.println(e);
			}
		}
	}
}

class nodeThread extends Thread {
	private String nodeID = null;
	// The input stream
	private ObjectInputStream is = null;
	// The output stream
	private ObjectOutputStream os = null;
	private Socket nodeSocket = null;
	private String[][] topology;
	private final nodeThread[] threads;
	private int maxNodesCount;
	private int nThreads;
	
	public nodeThread(Socket nodeSocket, nodeThread[] threads, String[][] topology, int extraThreads) {
		this.nodeSocket = nodeSocket;
		this.threads = threads;
		maxNodesCount = threads.length - extraThreads;
		nThreads = maxNodesCount + extraThreads;
		this.topology = topology;
	}
	
	public void run() {
		int maxNodesCount = this.maxNodesCount;
		nodeThread[] threads = this.threads;
		/* 
		 * The variables randomDrop, pktToDrop, numPktReceived and
		 * index are used to simulate message loss.
		 */
		int []randomDrop = new int[Common.ErrorRate];
		int pktToDrop = 0;
		int numPktReceived = 0;
		int index = 0;
		
		try {
			/* Create input and output streams for this client. */
			is = new ObjectInputStream(nodeSocket.getInputStream());
			os = new ObjectOutputStream(nodeSocket.getOutputStream());
		
			/* Get node ID */
			try {
				nodeID = ((StringPacket)is.readObject()).getString();
			} catch (ClassNotFoundException e) {
				System.err.println(e);
			}	
			
			/*
			 * Check if a thread corresponding to nodeID already exists. If it does, it indicates
			 * that nodeID might have disconnected and trying to connect again. In this case, set 
			 * the existing thread corresponding to nodeID to null.
			 */
			synchronized (this) {			
				for (int i = 0; i < nThreads; i++) {
					if(threads[i] != null && threads[i].getName().equals(nodeID)){
						threads[i] = null;
						break;
					}
				}				
				for (int i = 0; i < nThreads; i++) {
					if (threads[i] != null && threads[i] == this) {
						this.setName(nodeID);
						break;
					}
				}
			}
				
			/* Start Communication. */
			while (true) {
				PhyPacket rcvdPkt = null;
				try {
					rcvdPkt = (PhyPacket)is.readObject();
					if(numPktReceived == 0){
						Random random = new Random();
						randomDrop = RandomArrayGenerator.generateRandomArray(Common.ErrorRate, random);
						Arrays.sort(randomDrop);
						index = 0;
						pktToDrop = randomDrop[index];
					}
					numPktReceived++;
					if(numPktReceived == 100)
						numPktReceived = 0;
					
				} catch (ClassNotFoundException e) {
					System.err.println(e);
				}
				
				/* drop this packet and continue to the start of the loop */
				if(pktToDrop == numPktReceived){
					index++;
					if(index < Common.ErrorRate)
						pktToDrop = randomDrop[index];
					continue;
				}
				
				/* Retrieve destination */
				String dest = rcvdPkt.getDllPacket().getRlPacket().getAppPacket().getDst(); 
				
				/* Holds index of source into topology */
				int nodeIndex = 0;
				for(int i = 0; i < maxNodesCount; i++){
					if(topology[i][0].equals(nodeID)){
						nodeIndex = i;
						break;
					}
				}				
				
				/* Broadcast the message */
				if(dest.equals("0")){
					for(int i=1; i < topology[nodeIndex].length; i++){
						if(topology[nodeIndex][i] != null)
							synchronized (this) {
								String sendTo = topology[nodeIndex][i];
								for (int j = 0; j < nThreads; j++) {
									if (threads[j] != null && threads[j] != this
											&& threads[j].nodeID != null
											&& threads[j].nodeID.equals(sendTo)) {
												threads[j].os.writeObject(rcvdPkt);
									}
								}
							}							
					}
				}
				
				/* Send to individual destination */
				else{
					synchronized (this) {
						/* Check whether src and dest are connected */
						boolean found = false;
						for(int i=1; i < topology[nodeIndex].length; i++){
							if(topology[nodeIndex][i] != null && 
									topology[nodeIndex][i].equals(dest)){
								found = true;
								break;
							}
						}
						if(found){
							for (int i = 0; i < nThreads; i++) {
								if (threads[i] != null && threads[i] != this
										&& threads[i].nodeID != null
										&& threads[i].nodeID.equals(dest)) {
									threads[i].os.writeObject(rcvdPkt);
									break;
								}
							}
						}
					}
				}
			}
		} 
		catch (IOException e) {
		}
	}
}

/**
 * This class implements functionality to generate numElem random integers
 * from integers 1..MaxError. Generated numbers are returned as int[].
 */
class RandomArrayGenerator{
	  public static int[] generateRandomArray(int numElem, Random random){
		  int []array = new int[Common.MaxError];
		  int []randomArray = new int[numElem];
		  for(int i = 0; i < Common.MaxError; i++)
			  array[i] = i+1;
		  for(int i = 0; i < numElem; i++){
			  int randomIndex = generateRandomInteger(i, Common.MaxError-1, random); 
			  int temp = array[randomIndex];
			  array[randomIndex] = array[i];
			  array[i] = temp;
			  randomArray[i] = array[i];
		  }
		  return randomArray;
	  }
	  
	  private static int generateRandomInteger(int aStart, int aEnd, Random aRandom){
	    if ( aStart > aEnd ) {
	      throw new IllegalArgumentException("Start cannot exceed End.");
	    }
	    /* get the range, casting to long to avoid overflow problems */
	    long range = (long)aEnd - (long)aStart + 1;
	    /* compute a fraction of the range, 0 <= frac < range */
	    long fraction = (long)(range * aRandom.nextDouble());
	    int randomNumber =  (int)(fraction + aStart);  
	    return randomNumber;
	  }	
}


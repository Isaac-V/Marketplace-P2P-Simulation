// Isaac A. Vawter, SID: 28277700

// Package declaration
package peer;

// Import statements
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;

/*
 * The Buyer class represents a buyer peer in the Bazaar scenario. It functions as both a 
 * client and a server by sending adjacency messages and performing lookups as a client and  
 * receiving other buyer's lookups, seller replies, and adjacency messages as a server.
 */
public class Buyer {

	// Global variables
	protected int N; //Maximum number of peers
	protected String peerID; //This buyer's peerID
	protected ArrayList<String> neighbors; //This peer's neighbors
	protected PeerIDParser idParser; //A peerID parser
	protected ItemRequest itemRequest; //An ItemRequest monitor
	protected String outputFile; //An output file path
	
	// Constructor method that initializes global variables using provided parameters
	// and runs the init method.
	public Buyer(String peerID, 
			ArrayList<String> neighbors, 
			PeerIDParser idParser, 
			int N, 
			String outputFile) {
		this.peerID = peerID;
		this.neighbors = neighbors;
		this.idParser = idParser;
		this.N = N;
		this.outputFile = outputFile;
		this.itemRequest = new ItemRequest(idParser.getPeerIndex(peerID), outputFile);
		init();
	}
	
	// The init method creates a Listener thread to handle seller replies and forwarding
	// other buyer's lookup requests. This method also creates a Requester thread that 
	// performs lookup requests for this buyer.
	public void init(){
		Listener listener = new Listener(peerID, neighbors, idParser, itemRequest);
		listener.start();
		Requester requester = new Requester(peerID, neighbors, idParser, itemRequest, N, outputFile);
		requester.start();
	}
	
	/*
	 * The Listener class acts as a wrapper class to run a BuyerServer as its own thread.
	 */
	private static class Listener extends Thread{
		
		// Global variables that point to the same global variables as the Buyer object.
		private String peerID;
		private ArrayList<String> neighbors;
		private PeerIDParser idParser;
		ItemRequest itemRequest;
		
		
		// Constructor method that is provided global variables from the Buyer object to
		// initiate the Listener's global variables.
		public Listener(String peerID, 
				ArrayList<String> neighbors, 
				PeerIDParser idParser, 
				ItemRequest itemRequest){
			this.peerID = peerID;
			this.neighbors = neighbors;
			this.idParser = idParser;
			this.itemRequest = itemRequest;
		}
		
		// Run method that defines the execution of this thread.
		public void run(){
			
			// Initialize and enable a BuyerServer to process incoming messages to this buyer.
			BuyerServer buyerServer = new BuyerServer(idParser.getPeerPort(peerID), peerID, neighbors, idParser, itemRequest);
			try {
				buyerServer.enable();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}

	/*
	 * The Requester class acts as the client for this buyer and performs lookups to find
	 * sellers of the item this buyer is looking for.
	 */
	private static class Requester extends Thread{
		
		// Global variables that point to the same global variables as the Buyer object
		// with a few additions including lookup sequence numbers and a total purchase count.
		private int N;
		private String peerID;
		private ArrayList<String> neighbors;
		private PeerIDParser idParser;
		private ItemRequest itemRequest;
		private int seqNum;
		private int purchaseCount;
		private String outputFile;
		
		// Constructor method that is provided global variables from the Buyer object to
		// initiate the Listener's global variables. The sequence number and purchase count
		// are initialized to 0.
		public Requester(String peerID, 
				ArrayList<String> neighbors, 
				PeerIDParser idParser,
				ItemRequest itemRequest,
				int N,
				String outputFile){
			this.N = N;
			this.peerID = peerID;
			this.neighbors = neighbors;
			this.idParser = idParser;
			this.itemRequest = itemRequest;
			this.seqNum = 0;
			this.purchaseCount = 0;
			this.outputFile = outputFile;
		}
		
		// Run method that defines the execution of this thread.
		public void run(){
			
			// The Requester thread initially broadcasts adjacency messages to this buyer's neighbors. 
			// This allows its neighbors with lower peer indices to detect it and also consider this 
			// buyer a neighbor. These adjacency messages are preceded by the header string "adj".
			
			// Store any non-existent neighbors
			ArrayList<String> badNeighbors = new ArrayList<>();
			
			// Iterate through all neighbors
			for(int i = 0; i < this.neighbors.size(); i++){
				String neighborID = this.neighbors.get(i);
				
				try{
					
					// Create a socket connection a neighbor
					InetSocketAddress neighborAddr = idParser.getPeerSocket(neighborID); 
					Socket socket = new Socket();
					socket.connect(neighborAddr);
					
					// Create an outgoing message channel with the neighbor
					PrintWriter msgToNeighbor = new PrintWriter(socket.getOutputStream(), true);
					
					// Send neighbor an adjacency message with its peerID
					msgToNeighbor.println("adj");
					msgToNeighbor.println(peerID);
					
					// Close the socket connection
					socket.close();
				}
				catch(IOException e){ 
					System.out.println(e.getMessage()); 
					badNeighbors.add(neighborID); // Identify non-existent neighbors
				}
				
			}
			// Remove non-existing neighbors
			for(String badNeighbor : badNeighbors){
				this.neighbors.remove(badNeighbor);
			}
			
			// Perform lookups until 1000 successful purchases have occurred.
			while(this.purchaseCount < 1000){
				
				// Determine item for the next lookup
				String item = this.itemRequest.newRequest();
				
				// Perform lookup
				lookup(item, N);
				
				// Wait for responses
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				// Obtain and print responses to the output file
				Map<String, Boolean> responses = this.itemRequest.chooseWinner();
				printWinner(responses, item);
				
			}
			
			// After all requests have been fulfilled, print average response times and
			// output to the console a message indicating the buyer is finished buying
			// items.
			this.itemRequest.printAverageResponseTime();
			System.out.println("Done");
	
		}
		
		// The lookup method takes a product name and a hopcount and broadcasts the buyers
		// request to all of its neighbors. The product_name is the item being requested and
		// the hopcount is equal to the maximum number of peers in the network.
		private void lookup(String product_name, int hopcount){
			
			// Store any non-existent neighbors
			ArrayList<String> badNeighbors = new ArrayList<>();
			
			// Iterate through neighbors
			for(int i = 0; i < neighbors.size(); i++){
				String neighborID = neighbors.get(i);
				try{
					// Create a socket connection to a neighbor
					InetSocketAddress neighborAddr = idParser.getPeerSocket(neighborID);
					Socket socket = new Socket();
					socket.connect(neighborAddr);
					
					// Create an outgoing message channel with the neighbor
					PrintWriter msgToNeighbor = new PrintWriter(socket.getOutputStream(), true);
					
					// Send the neighbor a header message containing a lookup indicator "L" along
					// with a hopcount, product_name and sequence number. Then send the neighbor
					// the buyer's peerID.
					msgToNeighbor.println("L" + hopcount + ":" + product_name + "|" + seqNum);
					msgToNeighbor.println(peerID);
					
					// Close the socket connection
					socket.close();
				}			
				catch(IOException e){ 
					System.out.println(e.getMessage()); 
					badNeighbors.add(neighborID); //Identify non-existent neighbors
				}
			}
			// Remove non-existing neighbors
			for(String badNeighbor : badNeighbors){
				this.neighbors.remove(badNeighbor);
			}
			
			// Increase sequence number for the next lookup
			this.seqNum++;	
		}
		
		// The printWinner method prints out all the responses to a given lookup. It takes a 
		// map as a parameter that maps seller peerIDs with a boolean indicating if they were
		// chosen for the purchase of the item. It then prints out the results in this map
		// to an output file.
		private void printWinner(Map<String, Boolean> responses, String item){
			try{
				// Initialize a FileWriter object for writing to the output file.
				FileWriter writer = new FileWriter(outputFile + "buyerOutput.txt", true);
				
				// Create a request string to precede response results
				String request = "Peer ("+ this.idParser.getPeerIndex(peerID) +") ";
				request += item + " request: ";
				
				// If no sellers replied to the lookup indicate no responses were given
				if(responses.size() == 0){
					String noResponse = request + "No Response";
					writer.write(noResponse);
					writer.write(System.getProperty("line.separator"));
				}
				else{
					// Iterate through peerIDs of sellers that responded
					for(String sellerID : responses.keySet()){
						
						// Create a string indicating the peer that responded
						int sellerIndex = this.idParser.getPeerIndex(sellerID);
						String response = request + "Peer (" + sellerIndex + ") responds with " + item + " available";
						
						// Indicate if the seller was chosen for the purchase
						if(responses.get(sellerID)){
							response += " (chosen for purchase)";
							this.purchaseCount++;
						}
						
						// Write out the response string to the output file
						writer.write(response);
						writer.write(System.getProperty("line.separator"));
					}
					
				}
				
				// Close the FileWriter object
				writer.close();
			}			
			catch(IOException e){ System.out.println(e.getMessage()); }
		}
		
	}
	

}

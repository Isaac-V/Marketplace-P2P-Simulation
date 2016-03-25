// Isaac A. Vawter, SID: 28277700

// Package declaration
package peer;

// Import statements
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

/*
 * The Seller class represents a Seller peer in the Bazaar scenario. It functions as both a 
 * client and a server by sending adjacency messages and sending replies as a client and  
 * receiving buyer's lookups and adjacency messages as a server.
 */
public class Seller {

	// Global variables
	protected String peerID; //This seller's peerID
	protected ArrayList<String> neighbors; //This peer's neighbors
	protected PeerIDParser idParser; //A PeerIDParser object
	protected ItemStore itemStore; //An ItemStore monitor representing this seller's inventory
	protected String outputFile; //An output file path
	
	// Constructor method that initializes global variables from parameters passed by the main
	// method. Also initializes the ItemStore monitor for this seller and calls the init method.
	public Seller(String peerID, 
			ArrayList<String> neighbors, 
			PeerIDParser idParser, 
			String outputFile) {
		this.peerID = peerID;
		this.idParser = idParser;
		this.neighbors = neighbors;
		this.itemStore = new ItemStore(idParser.getPeerIndex(peerID), outputFile);
		this.outputFile = outputFile;
		init();
	}

	// The init method calls the neighborBroadcast method to broadcast adjacency messages and  
	// starts this seller's listening server by creating and enabling a SellerServer object.
	private void init(){
		
		// Performs adjacency broadcast
		neighborBroadcast();
		
		// Initializes and enables this seller's SellerServer
		int portNum = idParser.getPeerPort(peerID);
		SellerServer listener = new SellerServer(portNum, peerID, neighbors, idParser, itemStore);
		try{
			listener.enable();
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
	// The neighborBroadcast method broadcasts adjacency messages to this seller's neighbors. 
	// This allows its neighbors with lower peer indices to detect it and also consider this 
	// buyer a neighbor. These adjacency messages are preceded by the header string "adj".
	private void neighborBroadcast(){
		
		// Store any non-existent neighbors
		ArrayList<String> badNeighbors = new ArrayList<>();
		
		// Iterate through all neighbors
		for(int i = 0; i < neighbors.size(); i++){
			String neighborID = neighbors.get(i);
			
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
	}
	
}

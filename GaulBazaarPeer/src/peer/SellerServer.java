// Isaac A. Vawter, SID: 28277700

// Package declaration
package peer;

// Import statements
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

/*
 * The SellerServer class extends BaseServer and implements and executes SellerRequestHander threads 
 * to handle incoming lookup and adjacency messages.
 */
public class SellerServer extends BaseServer {

	// Global Variables that point to the same global variables that are used in the Seller object 
    // that created this SellerServer. Also contains a SequenceMap to implement flooding control.
	protected String peerID;
	protected PeerIDParser idParser;
	protected ArrayList<String> neighbors;
	protected ItemStore itemStore;
	protected SequenceMap seqMap;
	
	// Constructor method that initializes global variable from provided parameters as well as
    // creates a new SequenceMap monitor for flooding control.
	public SellerServer(int portNum, 
		String peerID, 
		ArrayList<String> neighbors, 
		PeerIDParser idParser,
		ItemStore itemStore) {
		super(portNum);
		this.peerID = peerID;
		this.neighbors = neighbors;
		this.idParser = idParser;
		this.itemStore = itemStore;
		this.seqMap = new SequenceMap();
	}
	
	// This implementation of the getServerThread returns a SellerRequestHandler thread that 
    // handles an incoming lookup or adjacency message to this buyer. The client socket is
    // used to create this handler thread.
	@Override
	public Thread getServerThread(Socket client) {
		return new ServerRequestHandler(client, peerID, neighbors, idParser, itemStore, seqMap);
	}
	
	/*
     * The SellerRequestHandler class uses the global objects provided by the Seller object to this
     * SellerServer to properly respond to incoming lookup and reply messages. Lookup messages are
     * automatically forwarded if they aren't requesting this seller's product, otherwise, this thread
     * will attempt to reserve the item in the ItemStore and send a reply to the buyer.
     */
	private static class ServerRequestHandler extends Thread{
		
		// Global variables that include several global variables from the original SellerServer 
        // object as well as the client socket for communication with the client.
		private Socket client;
		private String peerID;
		private PeerIDParser idParser;
		private ArrayList<String> neighbors;
		private ItemStore itemStore;
		private SequenceMap seqMap;
		
		// Constructor method that assigns the provided parameters to their corresponding global
        // variables.
		public ServerRequestHandler(Socket client,
				String peerID, 
				ArrayList<String> neighbors, 
				PeerIDParser idParser,
				ItemStore itemStore,
				SequenceMap seqMap) {
			this.client = client;
			this.peerID = peerID;
			this.idParser = idParser;
			this.neighbors = neighbors;
			this.itemStore = itemStore;
			this.seqMap = seqMap;
		}
		
		// Run method that defines the execution of this thread.
		public void run(){
			
			// Check to make sure the client socket is valid.
			if(client != null){
				
				try{
					// Initiate a BufferedReader to read the incoming messages from the client.
					BufferedReader msgFromClient = new BufferedReader(
			                new InputStreamReader(client.getInputStream()));
					
					// Read in the message header and the sourceID  of the message from the client
					String header = msgFromClient.readLine();
					String sourceID = msgFromClient.readLine();
					
					// Check if this message is an adjacency message, if so add the neighbor to 
                    // this Buyer's neigbor list.
					if(header.equals("adj")){
						neighbors.add(sourceID);
					}
					
					// Check if this message is a lookup message and handle it appropriately.
					else if(header.charAt(0) == 'L'){
						
						// Extract the sequence number from the header.
						int headerBreak1 = header.indexOf(':');
						int headerBreak2 = header.indexOf('|');
						int seqNum = Integer.parseInt(header.substring(headerBreak2 + 1, header.length()));
						
						// Check if this lookup has already been seen by this seller
						if(seqMap.updateSeqNum(idParser.getPeerIndex(sourceID), seqNum)){
							
							// Check if the lookup request product matches this seller's product, if so send reply
							if(itemStore.getItem().equals(header.substring(headerBreak1+1, headerBreak2)) &&
									itemStore.reserveProduct()){
								reply(sourceID);
							}
							// Otherwise forward the lookup
							else{
								
								// Extract hopcount
								int hopcount = Integer.parseInt(header.substring(1, headerBreak1));
								
								// Decrement hopcount and if hops remain, forward to all neighbors
								hopcount--;
								if(hopcount > 0){
									String nextHeader = "L" + hopcount + header.substring(headerBreak1, header.length());
									forward(nextHeader, sourceID);
								}
							}
						}
					}
				}
				catch(IOException e){ 
					e.printStackTrace();
				}
			}
			
	
		}
		
		// The reply message creates a socket connection with the buyer to negotiate a sale
		// of the product. Based on the buyer's response, this seller will either remove an
		// item from its reserved items or make the item available again.
		private void reply(String sourceID){
			try{
				// Create a socket connection with buyer
				InetSocketAddress buyerAddr = idParser.getPeerSocket(sourceID);
				Socket buyer = new Socket();
				buyer.connect(buyerAddr);
				
				// Create an outgoing message channel with buyer and send response message
				PrintWriter msgToBuyer = new PrintWriter(buyer.getOutputStream(), true);
				msgToBuyer.println("R:" + itemStore.getItem());
				msgToBuyer.println(peerID);
				
				// Create an incoming message channel with buyer
				BufferedReader msgFromBuyer = new BufferedReader(
		                new InputStreamReader(buyer.getInputStream()));
				
				// Read sale result and call the collectReservation method of the ItemStore
				String sale = msgFromBuyer.readLine();
				itemStore.collectReservation(sale.equals("BT"), idParser.getPeerIndex(sourceID));
				
				// Close the socket connection with the buyer
				buyer.close();
			}
			catch(IOException e){ 
				System.out.println(e.getMessage()); 
			}
		}
		
		// The forward method takes a lookup message's header and sourceID and forwards it
        // to every neighbor of this buyer.
		private void forward(String header, String sourceID){
			
			// Store any non-existent neighbors
			ArrayList<String> badNeighbors = new ArrayList<>();

			// Determine if the peer that sent the lookup has a larger peer index
			boolean bigSource = idParser.getPeerIndex(sourceID) > idParser.getPeerIndex(peerID);
			
			// Iterate through all neighbors
			for(int i = 0; i < neighbors.size(); i++){
				String neighborID = neighbors.get(i);
				
				// Determine if the neighbor has a larger peer index
				boolean bigNeighbor = idParser.getPeerIndex(neighborID) > idParser.getPeerIndex(peerID);
				
				// Forward the lookup message if the neighbor is in the opposite direction in the network
                // than the source of the message.
				if(bigSource ^ bigNeighbor){
					try{
						// Create a socket connection to a neighbor
						InetSocketAddress neighborAddr = idParser.getPeerSocket(neighborID);
						Socket socket = new Socket();
						socket.connect(neighborAddr);
						
						// Create an outgoing message channel with the neighbor and send the lookup message
						PrintWriter msgToNeighbor = new PrintWriter(socket.getOutputStream(), true);
						msgToNeighbor.println(header);
						msgToNeighbor.println(sourceID);
						
						// Close the socket
						socket.close();
					}			
					catch(IOException e){ 
						System.out.println(e.getMessage()); 
						badNeighbors.add(neighborID); //Identify non-existent neighbors
					}
				}
				
			}
			// Remove non-existing neighbors
			for(String badNeighbor : badNeighbors){
				this.neighbors.remove(badNeighbor);
			}
			
		}
		
	}


}

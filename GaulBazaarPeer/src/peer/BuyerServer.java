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
 * The BuyerServer class extends BaseServer and implements and executes BuyerRequestHander threads 
 * to handle incoming adjacency, lookup and reply messages.
 */
public class BuyerServer extends BaseServer {
	
	// Global Variables that point to the same global variables that are used in the Buyer object 
    // that created this BuyerServer. Also contains a SequenceMap to implement flooding control.
	protected String peerID;
	protected PeerIDParser idParser;
	protected ArrayList<String> neighbors;
	protected ItemRequest itemRequest;
	protected SequenceMap seqMap;
	
    // Constructor method that initializes global variable from provided parameters as well as
    // creates a new SequenceMap monitor for flooding control.
	public BuyerServer(int portNum, 
			String peerID, 
			ArrayList<String> neighbors, 
			PeerIDParser idParser, 
			ItemRequest itemRequest) {
		super(portNum);
		this.peerID = peerID;
		this.idParser = new PeerIDParser();
		this.neighbors = neighbors;
		this.itemRequest = itemRequest;
		this.seqMap = new SequenceMap();
	}

    
    // This implementation of the getServerThread returns a BuyerRequestHandler thread that 
    // handles an incoming lookup, reply, or adjacency message to this buyer. The client socket is
    // used to create this handler thread.
	@Override
	public Thread getServerThread(Socket client) {
		return new BuyerRequestHandler(client, peerID, neighbors, idParser, itemRequest, seqMap);
	}
	
	/*
     * The BuyerRequestHandler class uses the global objects provided by the Buyer object to this
     * BuyerServer to properly respond to incoming lookup and reply messages. Lookup messages are
     * automatically forwarded if they haven't been seen before and have remaining hops left and 
     * reply messages invoke the buy method, which nominates the seller to potentially be chosen 
     * for the purchase.
     */
	private static class BuyerRequestHandler extends Thread{
		
        // Global variables that include several global variables from the original BuyerServer 
        // object as well as the client socket for communication with the client.
		private Socket client;
		private String peerID;
		private PeerIDParser idParser;
		private ArrayList<String> neighbors;
		private ItemRequest itemRequest;
		private SequenceMap seqMap;
		
        // Constructor method that assigns the provided parameters to their corresponding global
        // variables.
		public BuyerRequestHandler(Socket client,
				String peerID, 
				ArrayList<String> neighbors, 
				PeerIDParser idParser, 
				ItemRequest itemRequest,
				SequenceMap seqMap) {
			this.client = client;
			this.peerID = peerID;
			this.idParser = idParser;
			this.neighbors = neighbors;
			this.itemRequest = itemRequest;
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
                        
                        // Extract the remaining hopcount and sequence number from the header.
						int headerBreak1 = header.indexOf(':');
						int headerBreak2 = header.indexOf('|');
						int hopcount = Integer.parseInt(header.substring(1, headerBreak1));
						int seqNum = Integer.parseInt(header.substring(headerBreak2 + 1, header.length()));
                        
                        // Decrement the hopcount.
						hopcount--;
                        
                        // If this lookup message hasn't already been forwarded and still has hops left
                        // then construct a new header for the lookup and forward to all neighbors.
						if(seqMap.updateSeqNum(idParser.getPeerIndex(sourceID), seqNum) && hopcount > 0){
							String nextHeader = "L" + hopcount + header.substring(headerBreak1, header.length());
							forward(nextHeader, sourceID);
						}
					}
                    
                    // Check if the message is a reply from a seller, if so, extract the item from the
                    // header and invoke the buy method.
					else if(header.charAt(0) == 'R'){
						int headerBreak = header.indexOf(':');
						String item = header.substring(headerBreak+1, header.length());
						buy(client, sourceID, item);
					}
					
				}
				catch(IOException e){ 
					e.printStackTrace();
				}
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
		
        // The buy method nominates a seller that has replied to this buyer's lookup
        // request to potentially be chosen for the purchase.
		private void buy(Socket seller, String sourceID, String item){
			try{
                // Initialize a PrintWriter object to send messages to the seller.
				PrintWriter msgToSeller = new PrintWriter(seller.getOutputStream(), true);
                
                // Check to see if this seller is chosen for the purchase and send either
                // a buy true "BT" or buy false "BF" message.
				if(this.itemRequest.buy(item, sourceID)){
					msgToSeller.println("BT");
				}
				else{
					msgToSeller.println("BF");
				}
			}
			catch(IOException e){ System.out.println(e.getMessage()); }
			
		}
		
	}

}

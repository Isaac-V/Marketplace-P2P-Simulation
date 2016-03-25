// Isaac A. Vawter, SID: 28277700

// Package declaration
package peer;

// Import statements
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/*
 * The ItemRequest monitor controls synchronization of a buyer's request for a certain product
 * and stores responses to a request from seller's of the item being bought. The response time
 * of each seller's reply message is also stored so the average response time of each request
 * can be calculated. Sellers must wait until the buyer chooses which of them to purchase the 
 * product from. Each buyer has only one ItemRequest monitor that is modified for each new
 * request.
 */
public class ItemRequest {

	// Global variables
	private static String item; // Product currently being requested
	private static Random rand; // Random object for determining seller to purchase from and which item is being requested
	private static int buyer; // The buyer's peer index
	private static long reqStart; // The System time at the issuance of a request
	private static ArrayList<Long> resTimes; // List of response times for a given request
	private static ArrayList<Double> avgTimes; // List of average response times from each request
	private static Map<String, Boolean> responses; // List of seller peerIDs and a boolean indicating if they are chosen for the purchase
	private static Map<String, Boolean> oldResponses; // List of responses to the previous request used for messages to sellers
	private static String outputFile; // Output file path for this buyer
	private static boolean active; // Indicates whether this ItemRequest is receiving replies from sellers

	// Contructor method that takes in the buyer's peer index and output file path and initiates
	// all global variables.
	@SuppressWarnings("static-access")
	public ItemRequest(int buyer, String outputFile) {
		this.buyer = buyer;
		this.rand = new Random();
		this.reqStart = 0;
		this.resTimes = null;
		this.item = "";
		this.outputFile = outputFile;
		this.active = false;
		this.responses = null;
		this.oldResponses = null;
		this.avgTimes = new ArrayList<>();
	}
	
	// The resetItem method picks a new item to request, it requires the new item to not be the same
	// as the previous item that was requested. 
	@SuppressWarnings("static-access")
	public synchronized void resetItem(){
		String nextItem = this.item;		
		while(nextItem.equals(this.item)){
			switch(rand.nextInt(3)){
			case 0 : this.item = "boar";
					 break;
			case 1 : this.item = "fish";
				     break;
			case 2 : this.item = "salt";
				     break;
			}
		}
	}
	
	// The buy method takes in a product name and the peerID of the seller and adds that
	// seller to the responses Map. The thread created by a reply message to the BuyerServer
	// then waits until a seller is chosen for the purchase. After which it notifies the
	// seller who sent that reply if they were chosen or not.
	@SuppressWarnings("static-access")
	public synchronized boolean buy(String item, String sellerID){
		
		// Check that the item being sold is the same as the item requested and that the
		// ItemRequest is active. Return false otherwise.
		if(this.item.equals(item) && this.active){
			
			// Add the seller to the response map, calculate response time and wait for a 
			// purchase decision.
			this.responses.put(sellerID, false);
			this.resTimes.add(System.currentTimeMillis() - this.reqStart);
			try{
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// After a purchase decision is made the response information is migrated to
			// the oldResponses map, which is used for notifying the sellers of the decision.
			// Also checks to make sure the oldResponse map exists.
			if(this.oldResponses != null){
				return this.oldResponses.get(sellerID);
			}
			else{
				return false;
			}
		}
		else return false;
	}
	
	// The chooseWinner method is used by the Buyer object to determine which of the sellers
	// that replied to this request to purchase the product from. Returns the entire map of
	// responses so that the Buyer object can print the results to a file.
	@SuppressWarnings("static-access")
	public synchronized Map<String, Boolean> chooseWinner(){
		
		// Initialize a new oldResponses map and deactivate this ItemRequest
		this.oldResponses = new HashMap<>();
		this.active = false;
		
		// Check to see if any responses were made
		if(!this.responses.isEmpty()){
			
			// Pick a random index of the winner
			int winner = rand.nextInt(this.responses.keySet().size());
			
			// Iterate through the sellers that responded and mark the one that was chosen for
			// the purchase, copy this information to the oldResponses map.
			int count = 0;
			for(String seller : this.responses.keySet()){
				if(count == winner){
					this.responses.put(seller, true);
					this.oldResponses.put(seller, true);
				}
				else{
					this.oldResponses.put(seller, false);
				}
				count++;
			}
			
			// Calculate average response time to this request
			double resTotal = 0.0;
			for(int i = 0; i < this.resTimes.size(); i++){
				resTotal += this.resTimes.get(i);
			}
			double resAvg = resTotal/this.resTimes.size();
			this.avgTimes.add(resAvg);
			
			// Wake all threads waiting in the buy method.
			notifyAll();
		}
		
		// Return a pointer to the response map to the Buyer object
		return this.responses;
	}
	
	
	// Getter method for the ItemRequests current product
	@SuppressWarnings("static-access")
	public synchronized String getItem(){
		return this.item;
	}

	// The newRequest method is used by the buyer to prepare the ItemRequest
	// before a lookup is made.
	@SuppressWarnings("static-access")
	public synchronized String newRequest(){
		
		// Choose a new item
		resetItem();
		
		// Print the new item request to the output file
		String request = "Peer ("+ buyer +") " + this.item + " request: Issued";
		try{
			FileWriter writer = new FileWriter(outputFile + "buyerOutput.txt", true);
			writer.write(request);
			writer.write(System.getProperty("line.separator"));
			writer.close();
		}
		catch(IOException e){ e.printStackTrace(); }
		
		// Activate the ItemRequest, initialize the request start time, the response time
		// array, and the seller response Map (this Map is reinitialized upon each new request).
		// Returns the product name to the buyer for generating its lookup request.
		this.active = true;
		this.reqStart = System.currentTimeMillis();
		this.resTimes = new ArrayList<>();
		this.responses = new HashMap<>();
		return this.item;
	}
	
	// The printAverageResponseTime is used to print all the request average response times
	// as well as an overall average response time for the buyer.
	@SuppressWarnings("static-access")
	public synchronized void printAverageResponseTime(){
		
		// Start total for the overall average response time
		double overallAvg = 0.0;
		try{
			// Create a FileWriter object for writing the response times
			FileWriter writer = new FileWriter(outputFile + "buyerAvgResTimes.txt", true);
			
			// Iterate through the average response times, printing them out to the output file
			for(Double avg : this.avgTimes){
				overallAvg += avg;
				writer.write(avg.toString());
				writer.write(System.getProperty("line.separator"));
			}
			
			// Calculate and write the overall average response time for the buyer
			writer.write("Overall Response Time Average: " + overallAvg/this.avgTimes.size() + "ms");
			writer.write(System.getProperty("line.separator"));
			
			// Close the FileWriter object
			writer.close();
		}
		catch(IOException e){ e.printStackTrace(); }
	}

}

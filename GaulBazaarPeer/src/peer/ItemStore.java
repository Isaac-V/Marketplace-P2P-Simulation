// Isaac A. Vawter, SID: 28277700

// Package declaration
package peer;

// Import statements
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

/*
 * The ItemStore monitor is used by a seller to synchronously track its inventory and
 * sell products to buyers. It places products in reserve for each reply sent to a 
 * buyer's lookup, and upon the buy response, either removes that product from its
 * inventory or marks it as available again. This allows the seller to not accidentally
 * sell one buyer's product to another buyer or over-sell its inventory.
 */
public class ItemStore {
	
	// Global variables
	private static int seller; // The seller's peer index
	private static String item; // The product currently being sold
	private static int available; // The remaining product that is available
	private static int reserved; // The product that is reserved for buyer's buy responses
	private static Random rand; // A Random object for determining g which product is to be sold
	private static String outputFile; // An output file path
	
	// Constructor method that is provided with a seller peer index and output file from the Seller
	// object that creates this ItemStore. This also initializes all global variables and calls the
	// resetItem method to select a new item and amount to be sold.
	@SuppressWarnings("static-access")
	public ItemStore(int seller, String outputFile){
		this.seller = seller;
		this.item = "";
		this.available = 0;
		this.reserved = 0;
		this.rand = new Random();
		this.outputFile = outputFile;
		resetItem();
	}
	
	// The resetItem method chooses a new product to be sold at this ItemStore and also determines
	// the amount of available inventory for that product. It ensures that the item is chosen randomly
	// and is not the same item that was sold previously. It also ensures the amount of the item 
	// available is randomly chosen and not zero.
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
		while(this.available == 0){
			this.available = rand.nextInt(10);
		}
	}
	
	// The reserve product method is called by a SellerServer thread that is responding to a lookup 
	// request for an item that is sold at this ItemStore. The method reduces the available item by
	// one and adds one to the reserved items. The method returns true if there are available items
	// and false if otherwise.
	@SuppressWarnings("static-access")
	public synchronized boolean reserveProduct(){
		if(this.available > 0){
			this.available--;
			this.reserved++;
			return true;
		}
		else return false;
	}
	
	// The collectReservation method is called by a SellerServer thread in response to a buyer's
	// buy decision. If the buyer bought this seller's product this method prints the results
	// of the sale to an output file and removes an item from the reserved items. If this totally
	// depletes the store of all its items, this method calls the resetItem method to re-stock the 
	// ItemStore with new items. If the buyer did not buy the product and item is moved from reserved
	// and added to available.
	@SuppressWarnings("static-access")
	public synchronized boolean collectReservation(boolean indicator, int buyer){
		
		// Check that an item is in fact reserved
		if(this.reserved > 0){
			
			// Check if this ItemStore's product was purchased
			if(indicator){
				
				// Remove the product from reserved items and write the sale result to the output file
				this.reserved--;
				try{
					FileWriter writer = new FileWriter(outputFile, true);
					writer.write("Peer (" + seller + ") sold " + item + " to Peer (" + buyer + "), remaining inventory: " + (available + reserved));
					writer.write(System.getProperty("line.separator"));
					writer.close();
				}
				catch(IOException e){ e.printStackTrace(); }
				
				// If no more inventory left, re-stock the ItemStore with new product
				if(this.available == 0 && this.reserved == 0){
					resetItem();
				}
			}
			else{
				
				// If this ItemStore's product wasn't purchased, make that product available again
				this.reserved--;
				this.available++;
			}
			return true;
		}
		else return false;
	}
	
	// Getter method for returning this ItemStore's current product
	@SuppressWarnings("static-access")
	public synchronized String getItem(){
		return this.item;
	}

}

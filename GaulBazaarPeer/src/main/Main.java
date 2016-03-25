// Isaac A. Vawter, SID: 28277700

// Package declaration
package main;

// Import statements
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

import peer.Buyer;
import peer.PeerIDParser;
import peer.Seller;

/*
 * Peer Main class that contacts the Registry as a client to receive peer information,
 * decides if the peer is a buyer or seller, and then initializes the peer.
 */
public class Main {
	
	//Main method
	@SuppressWarnings("unused")
	public static void main(String[] args) throws IOException{
		
		// Variables used for initializing peers including maximum number of peers, peerID
		// neighbor peerIDs, and an peerID parser.
		int N;
		String peerID;
		ArrayList<String> neighbors = new ArrayList<>();
		PeerIDParser idParser = new PeerIDParser();
		
		// Create a socket connection with the Registry Server, IP address and port must be
		// hard-coded
		Socket regServer = new Socket("98.217.50.221", 10250); // Must be hard-coded with RegistryServer's IP
		
		// Create a BufferedReader object to receive information from the Registry Server
		BufferedReader msgFromServer = new BufferedReader(
                new InputStreamReader(regServer.getInputStream()));
		
		// Read peerID from the Registry Server, terminate if maximum number of peers has 
		// already been reached
		peerID = msgFromServer.readLine();
		if(peerID.equals("terminate")){
			regServer.close();
			return;
		}
		
		// Iterate through all neighbor peerIDs from the Registry Server and add to the 
		// neighbors ArrayList
		String neighborID = msgFromServer.readLine();
		while(!neighborID.equals("end")){
			neighbors.add(neighborID);
			neighborID = msgFromServer.readLine();
		}
		
		// Read the maximum number of peers from Registry Server
		N = Integer.parseInt(msgFromServer.readLine());
		
		// Close connection with Registry Server
		regServer.close();

		
		// Initialize a Random object for determining if this peer is a buys or seller as well as
		// an outputFile string for the peer
		Random rand = new Random();
		String outputFile = System.getProperty("user.dir"); 
		outputFile += File.separator + "Peer" + idParser.getPeerIndex(peerID);
		
		// Randomly determine if the peer is a buyer or seller and initialize the corresponding peer object
		if(rand.nextDouble() < 0.5){
			System.out.println("Buyer ID: " + peerID);
			Buyer buyer = new Buyer(peerID, neighbors, idParser, N, outputFile);
		}
		else{
			System.out.println("Seller ID: " + peerID);
			Seller seller = new Seller(peerID, neighbors, idParser, outputFile + "sellerOutput.txt");
		}
		
	}

}
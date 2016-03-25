// Isaac A. Vawter, SID: 28277700

// Package declaration
package main;

// Import statements
import server.PeerRegistry;
import server.RegistryServer;

import java.io.IOException;
import java.net.UnknownHostException;

/*
 * Registry Main class, where the total number of peers (N) and the number of
 * neighbors for each peer (2*neighborRadius) is defined. The registry server will
 * provide each new peer with neighbors. This is only somewhat dynamic and assumes 
 * peers will stay online indefinitely.
 */
public class Main {
	
	//Main method
	public static void main(String[] args) throws UnknownHostException{
		
		// Define the total number of peers, neighborRadius of each peer, and RegistryServer port number
		int N = 10;
		int neighborRadius = 3;
		int portStart = 10250;
		
		// Create a new PeerRegistry monitor to store peer listening sockets in a peer index
		PeerRegistry registry = new PeerRegistry(N, neighborRadius, portStart);
		
		// Create a new RegistryServer that is contacted by peers to receive their
		// peerID and neighbors
		RegistryServer regServer = new RegistryServer(portStart, registry, N);
		
		// Activate the RegistryServer
		try{
			regServer.enable();
		}
		catch(IOException e){ System.out.println(e.getMessage()); }
		
	}
	
}

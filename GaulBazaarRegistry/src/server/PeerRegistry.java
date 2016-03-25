// Isaac A. Vawter, SID: 28277700

// Package declaration
package server;

// Import statements
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;

/*
 * The PeerRegistry monitor maintains a list of all peers by their listening sockets and
 * provides neighbors for peers when an addPeer method is called. All methods are synchronized 
 * to prevent issues that could arise if multiple new peers are accessing the registry
 * at the same time.
 */
public class PeerRegistry {
	
	// Global variables that track the maximum number of peers, the neighbor radius for each
	// peer, what port number to assign the next new peer, and a list of registered peers
	private static int peerLimit;
	private static int neighborRadius;
	private static int nextPeerServerPort;
	private static ArrayList<InetSocketAddress> socketAddrs;
	
	// Constructor method that takes in a maximum number of peers, neighbor radius and starting
	// port number to initialize global variables.
	@SuppressWarnings("static-access")
	public PeerRegistry(int size, int neighborRadius, int portStart){
		this.peerLimit = size;
		this.neighborRadius = neighborRadius;
		this.nextPeerServerPort = portStart;
		this.socketAddrs = new ArrayList<>();
	}
	
	// The addPeer method takes in the IP address of a new peer and creates a new socket address
	// for them and adds them to the registry. It then returns the peer's index in the registry
	// as well as their listening port. Returns null if the maximum number of peers has already
	// been reached.
	@SuppressWarnings("static-access")
	public synchronized int[] addPeer(InetAddress addr){
		if(socketAddrs.size() <= peerLimit){
			this.nextPeerServerPort++; // Next available port
			socketAddrs.add(new InetSocketAddress(addr, this.nextPeerServerPort)); // Create socket address
			int[] portData = {socketAddrs.size() - 1, this.nextPeerServerPort}; // Peer index and listening port
			return portData;
		}
		else return null;
	}
	
	// The getNeighbors method takes a peer's index and provides their neighbors socket addresses.
	// This method only provides neighbors with lower indices that the provided peer. Future peers
	// will connect with this peer as well if it is within their neighbor radius. Returns a list
	// of peerIDs.
	public synchronized ArrayList<String> getNeighbors(int peerIndex){
		if(peerIndex < 0 || peerIndex > socketAddrs.size()){ //Check that peer index is valid
			throw new IllegalArgumentException("Neighbor must be an existing peer.");
		}
		else{
			ArrayList<String> neighborIDs = new ArrayList<>(); // Initialize list to be returned
			for(int i = 1; i <= neighborRadius && peerIndex - i >= 0; i++){ //Iterate through neighbor radius
				int neighborIndex = peerIndex - i; // Determine peer index
				neighborIDs.add(socketAddrs.get(peerIndex - i).toString() + "|" + neighborIndex); //Construct and add neighbor peerID to list
			}
			return neighborIDs; //return list
		}
	}

}

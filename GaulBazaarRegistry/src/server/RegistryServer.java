// Isaac A. Vawter, SID: 28277700

// Package declaration
package server;

// Import statements
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;

/*
 * The RegistryServer class extends BaseServer and implements a handler (ClientRegistration) that 
 * allows clients to connect with it and become a peer in the Bazaar network.
 */
public class RegistryServer extends BaseServer {

	// Global variables including one PeerRegistry monitor and a maximum number of peers
	protected PeerRegistry registry;
	protected int N;
	
	// Constructor method that takes in a port number, PeerRegistry, and maximum number of peers
	// to satisfy the BaseServer constructor and set the global variables.
	public RegistryServer(int portNum, PeerRegistry registry, int N) {
		super(portNum);
		this.N = N;
		this.registry = registry;
	}

	// The getServerThread method logs when a new client connects to become a peer. It also creates
	// a new thread to handle the client's registration. The method is provided with a socket connection
	// that the client established with this listening server, which is used to create a ClientRegistration
	// thread.
	@Override
	public Thread getServerThread(Socket client) {
		log.log(Level.INFO, "Client connected, remote socket: " + client.getRemoteSocketAddress());
		return new ClientRegistration(client, registry, N);
	}
	
	
	/*
	 * The ClientRegistration class is used to create objects that handle clients of the RegistryServer.
	 * Each new client connection establishes a new Thread that then handles accessing the PeerRegistry
	 * to obtain that clients peerID and neighbors. The ClientRegistration uses a socket connection to
	 * send a client's peer information to them so they can begin buying or selling.
	 */
	private static class ClientRegistration extends Thread{
		
		// Global variables that include the socket with the client, the PeerRegistry monitor, and the
		// maximum number of peers.
		private Socket client;
		private PeerRegistry registry;
		private int N;
		
		// Constructor method that takes in parameters to set the global variables
		public ClientRegistration(Socket client, PeerRegistry registry, int N){
			this.client = client;
			this.registry = registry;
			this.N = N;
		}
		
		// Run method that defines the execution of this thread
		public void run(){
			
			try{
				
				// Create a PrintWriter object to communicate with the client
                PrintWriter msgToClient = new PrintWriter(client.getOutputStream(), true);
                
                // Determine the client's IP address from the socket connection
                InetSocketAddress peerSocket = (InetSocketAddress)client.getRemoteSocketAddress();
                InetAddress peerIP = peerSocket.getAddress();
                
                // Get the peer index and listening port number for the client
                int[] peerIndexAndPort = registry.addPeer(peerIP);
                
                // If there is no more room for any more peers, a terminate message is sent to the client.
                if(peerIndexAndPort == null){
                	msgToClient.println("terminate");
                }
                // Otherwise, construct and send the client's peerID
                else{
                	String peerID = new InetSocketAddress(client.getInetAddress(), peerIndexAndPort[1]).toString();
                	peerID += "|" + peerIndexAndPort[0];
                	msgToClient.println(peerID);
                	
                	// Send the client their neighbor's peerIDs one at a time, followed by an end message
                	ArrayList<String> neighborIDs = registry.getNeighbors(peerIndexAndPort[0]);
            		for(String neighbor : neighborIDs){
            			msgToClient.println(neighbor);
            		}
            		msgToClient.println("end");
            		
            		// Send the client the maximum number of peers for hopcount determination
            		msgToClient.println("" + N);

                }
				
			}
			catch(IOException e){ System.out.println(e.getMessage()); }

		}
		
	}

}

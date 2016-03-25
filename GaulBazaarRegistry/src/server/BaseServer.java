// Isaac A. Vawter, SID: 28277700

// Package declaration
package server;

// Import statements
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * The BaseServer abstract class provides a basic implementation that can be extended
 * to create a server that handles multiple requests simultaneously. It also logs when
 * the BaseServer is listening and on what port.
 */
public abstract class BaseServer {
	
	// Global log and listening socket
	protected Logger log;
	private ServerSocket socket;
	
	// Constructor method that creates a BaseServer that listens on a socket
	// with the provided port number
	public BaseServer(int portNum){
		
		this.log = Logger.getGlobal();
		try{
			this.socket = new ServerSocket(portNum);
		} 
		catch(IOException e){ System.out.println(e.getMessage()); }
	}
	
	// The enable method activates the server so that it starts listening on this
	// server's socket and creates a thread whenever a client connects to it
	public void enable() throws IOException{
		try{
			log.log(Level.INFO, "Server Listening on Port " + socket.getLocalPort());
			while(true){
				getServerThread(this.socket.accept()).start();
			}
		}
		catch(IOException e){ System.out.println(e.getMessage()); }
		finally{
			socket.close();
		}
	}
	
	
	// Getter method for this server's listening port number
	public void getPort(){
		this.socket.getLocalPort();
	}
	
	// Abstract method that an subclass will implement to handle incoming client requests
	public abstract Thread getServerThread(Socket client);
	
}

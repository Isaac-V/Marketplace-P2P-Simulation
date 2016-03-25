// Isaac A. Vawter, SID: 28277700

// Package declaration
package peer;

// Import Statements
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/*
 * The PeerIDParser class is used to provide methods that allow important peer information
 * to be parsed out of its peerID string. That information includes a peer's listening port,
 * IP address, socket address, and peer index.
 */
public class PeerIDParser {

	// Constructor method that takes no parameters.
	public PeerIDParser() {}
	
	// The getPeerPort method parses a peer's listening port from its peerID, the listening
	// port is the substring between the ':' and '|' characters.
	public int getPeerPort(String peerID){
		return Integer.parseInt(peerID.substring(peerID.indexOf(':')+1, peerID.indexOf('|')));
	}
	
	// The getPeerIP method parses a peer's IP address from its peerID, the IP Address
	// is the substring from the 2nd letter to just before the ':' character. Throws
	// and exception if the IP address is invalid.
	public InetAddress getPeerIP(String peerID) throws UnknownHostException{
		return InetAddress.getByName(peerID.substring(1, peerID.indexOf(':')));
	}
	
	// The getPeerSocket method uses the getPeerPort and getPeerIP methods to construct a
	// socket address from the peer's IP address and listening port. Throws and exception 
	// if the IP address is invalid.
	public InetSocketAddress getPeerSocket(String peerID) throws UnknownHostException{
		return new InetSocketAddress(getPeerIP(peerID), getPeerPort(peerID));
	}
	
	// The getPeerIndex method parses a peer's index from its peerID, the peer index is the 
	// substring between the '|' character and the end of the peerID string.
	public int getPeerIndex(String peerID){
		return Integer.parseInt(peerID.substring(peerID.indexOf('|') + 1, peerID.length()));
	}

}

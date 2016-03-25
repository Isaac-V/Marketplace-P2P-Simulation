// Isaac A. Vawter, SID: 28277700

// Package declaration
package peer;

// Import statements
import java.util.HashMap;
import java.util.Map;

/*
 * The SequenceMap monitor maintains a map of peerIDs to lookup sequence numbers to prevent
 * double forwards or double replies resulting from an incoming lookup request.
 */
public class SequenceMap {

	// Global map variable of peer index to sequence number
	private static Map<Integer, Integer> seqNumbers;
	
	// Constructor method that initializes the seqNumbers global variable
	@SuppressWarnings("static-access")
	public SequenceMap() {
		this.seqNumbers = new HashMap<>();
	}
	
	// The updateSeqNum method takes a peer index and sequence number and checks if the sequence
	// number is higher than the last one seen from that peer. It returns true if this lookup hasn't
	// been seen before or false if it has.
	@SuppressWarnings("static-access")
	public synchronized boolean updateSeqNum(int peerIndex, int seqNum){
		
		// Check if this peer index has been seen before
		if(seqNumbers.keySet().contains(peerIndex)){
			
			// Retrieve the most recently seen sequence number and compare it to the seqNum parameter,
			// updating the seqNumbers Map returning true and if the parameter is larger and false if otherwise.
			int oldSeq = seqNumbers.get(peerIndex);
			if(oldSeq >= seqNum){
				return false;
			}
			else{
				seqNumbers.put(peerIndex, seqNum);
				return true;
			}
		}
		// If the peer index has not been seen before add it and the sequence number to the seqNumbers
		// Map and return true.
		else{
			this.seqNumbers.put(peerIndex, seqNum);
			return true;
		}
		
	}
	

}

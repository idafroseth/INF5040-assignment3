package no.uio.inf5040.gossip;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;

import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.transport.Transport;
import peersim.core.CommonState;


/**
 * @author Lucas Provensi
 * 
 * Basic Shuffling protocol template
 * 
 * The basic shuffling algorithm, introduced by Stavrou et al in the paper: 
 * "A Lightweight, Robust P2P ////System to Handle Flash Crowds", is a simple 
 * peer-to-peer communication model. It forms an overlay and keeps it 
 * connected by means of an epidemic algorithm. The protocol is extremely 
 * simple: each peer knows a small, continuously changing set of other peers, 
 * called its neighbors, and occasionally contacts a random one to exchange 
 * some of their neighbors.
 * 
 * This class is a template with instructions of how to implement the shuffling
 * algorithm in PeerSim.
 * Should make use of the classes Entry and GossipMessage:
 *    Entry - Is an entry in the cache, contains a reference to a neighbor node
 *  		  and a reference to the last node this entry was sent to.
 *    GossipMessage - The message used by the protocol. It can be a shuffle
 *    		  request, reply or reject message. It contains the originating
 *    		  node and the shuffle list.
 *
 */
public class BasicShuffle implements Linkable, EDProtocol, CDProtocol{
	
	private static final String PAR_CACHE = "cacheSize";
	private static final String PAR_L = "shuffleLength";
	private static final String PAR_TRANSPORT = "transport";

	private final int tid;

	// The list of neighbors known by this node, or the cache.
	private List<Entry> cache;
	
	// The maximum size of the cache;
	private final int size;
	
	// The maximum length of the shuffle exchange;
	private final int l;
	
	private boolean waitForReply;
//	private LinkedList<Entry> lastSentSubset;
	
	/**
	 * Constructor that initializes the relevant simulation parameters and
	 * other class variables.
	 * 
	 * @param n simulation parameters
	 */
	public BasicShuffle(String n)
	{	
		this.size = Configuration.getInt(n + "." + PAR_CACHE);
		this.l = Configuration.getInt(n + "." + PAR_L);
		this.tid = Configuration.getPid(n + "." + PAR_TRANSPORT);
		cache = new ArrayList<Entry>(size);
		waitForReply = false;
	}

	/* START YOUR IMPLEMENTATION FROM HERE
	 * 
	 * The simulator engine calls the method nextCycle once every cycle 
	 * (specified in time units in the simulation script) for all the nodes.
	 * 
	 * You can assume that a node initiates a shuffling operation every cycle.
	 * 
	 * @see peersim.cdsim.CDProtocol#nextCycle(peersim.core.Node, int)
	 */
	
	public void nextCycle(Node node, int protocolID) {
		// 1. If P is waiting for a response from a shuffling operation initiated in a previous cycle, return;
		if(waitForReply){
			return;
		}
		// 2. If P's cache is empty, return;	
		else if(cache.isEmpty()){
			return;
		}	
			//3. Select a random neighbor (named Q) from P's cache to initiate the shuffling;
		Entry q = cache.get(CommonState.r.nextInt(cache.size()));
		// 4. If P's cache is full, remove Q from the cache;
		if(cache.size()>=size){
			cache.remove(q);
		}
		// 5. Select a subset of other l - 1 random neighbors from P's cache;
		//lastSentSubset = null;
		//lastSentSubset = 
		// 6. Add P to the subset;
		//lastSentSubset.add(new Entry(node));
		// 7. Send a shuffle request to Q containing the subset;
		GossipMessage message = new GossipMessage(node, generateSubset(l-1, q.getNode()));
		message.setType(MessageType.SHUFFLE_REQUEST);
		Transport tr = (Transport) node.getProtocol(tid);
		tr.send(node, q.getNode(), message, protocolID);
		// 8. From this point on P is waiting for Q's response and will not initiate a new shuffle operation;
		waitForReply = true;
		
	}


	/* The simulator engine calls the method processEvent at the specific time unit that an event occurs in the simulation.
	 * It is not called periodically as the nextCycle method.
	 * 
	 * You should implement the handling of the messages received by this node in this method.
	 * 
	 * @see peersim.edsim.EDProtocol#processEvent(peersim.core.Node, int, java.lang.Object)
	 */
	public void processEvent(Node node, int pid, Object event) {
		// Let's name this node as Q;
		// Q receives a message from P;
		//	  - Cast the event object to a message:
		GossipMessage message = (GossipMessage) event;
		Node p = message.getNode();
		Node q = node;
	
		switch (message.getType()) {
			case SHUFFLE_REQUEST:
//				  1. If Q is waiting for a response from a shuffling initiated in a previous cycle, send back to P a message rejecting the shuffle request; 
				if(waitForReply){
					GossipMessage reject = new GossipMessage(node, null);
					reject.setType(MessageType.SHUFFLE_REJECTED);
					Transport tr = (Transport) node.getProtocol(tid);
					tr.send(q, p, reject, pid);
					return;
				}
//				  2. Q selects a random subset of size l of its own neighbors;
//				lastSentSubset = null;
//				lastSentSubset = generateSubset(l, p);
				//	  3. Q reply P's shuffle request by sending back its own subset;
				GossipMessage reply = new GossipMessage(q, generateSubset(l,p));
				reply.setType(MessageType.SHUFFLE_REPLY);
				Transport tr = (Transport) q.getProtocol(tid);
				tr.send(q, p, reply, pid);
				//  4. Q updates its cache to include the neighbors sent by P:
				updateCache(message);
				//updateCache(p,message.getShuffleList());
				break;		
			case SHUFFLE_REPLY:
				//	  2. Q updates its cache to include the neighbors sent by P:
				updateCache(message);
				for (Entry entry : cache) {
					entry.setSentTo(null);
				}
				if(!contains(p)&&cache.size()<size){
					cache.add(new Entry(p));
				}
				//	 3. Q is no longer waiting for a shuffle reply;	 
				waitForReply = false;
				break;
			case SHUFFLE_REJECTED:
				for (Entry entry : cache) {
					entry.setSentTo(null);
				}
				
				if(!contains(p)&&cache.size()<size){
					cache.add(new Entry(p));
				}
				waitForReply = false;
				break;
				
			default:
				break;
		}
	}
	/**
	 * Generates a random subset from the cache.  
	 * @param src
	 * @param dest
	 * @return
	 */
	private LinkedList<Entry> generateSubset(Integer length, Node dest){
		LinkedList<Entry> subset = new LinkedList<Entry>();
		List<Entry> copyOfCache = new ArrayList<Entry>(cache);
		copyOfCache.remove(dest);
		for(int i =0; i<length && !copyOfCache.isEmpty(); i++){
			Entry neighbor = copyOfCache.remove(CommonState.r.nextInt(copyOfCache.size()));
			neighbor.setSentTo(dest);
			subset.add(new Entry(neighbor.getNode()));
		}
		return subset;
	}
	/**
	 * Updates the cache based on these rules: 
	 *  - No neighbor appears twice in the cache
	 *  - Use empty cache slots to add the new entries
	 *  - If the cache is full, you can replace entries among the ones sent to P with the new ones
	 * @param message
	 */
	private void updateCache(GossipMessage message){
		ArrayList<Entry> recievedNeighbors = (ArrayList<Entry>) message.getShuffleList();
		LinkedList<Entry> lastSentNodes = new LinkedList<Entry>();
		
		for(Entry e: cache){
			if(e.getSentTo()!=null){
				if(e.getSentTo().equals(message.getNode())){
					lastSentNodes.add(e);
				}
			}
		}
		
		for(Entry neighbor : recievedNeighbors){
			//First we must check if the node is in the list
			if(!contains(neighbor.getNode())){
				if(cache.size()<size){
					cache.add(neighbor);
				}else{
					if(!lastSentNodes.isEmpty()){
						cache.remove(lastSentNodes.removeFirst());
						cache.add(neighbor);
					}
				}
			}
		}
	}
	
/* The following methods are used only by the simulator and don't need to be changed */
	
	public int degree() {
		return cache.size();
	}

	public Node getNeighbor(int i) {
		return cache.get(i).getNode();
	}

	public boolean addNeighbor(Node neighbour) {
		if (contains(neighbour)){
			return false;
		}
		if (cache.size() >= size){
			return false;
		}
		Entry entry = new Entry(neighbour);
		cache.add(entry);
		return true;
	}
	
	public boolean contains(Node neighbor) {
		return cache.contains(new Entry(neighbor));
	}

	public Object clone()
	{
		BasicShuffle gossip = null;
		try { 
			gossip = (BasicShuffle) super.clone(); 
		} catch( CloneNotSupportedException e ) {
			
		} 
		gossip.cache = new ArrayList<Entry>();

		return gossip;
	}

	public void onKill() {
		// TODO Auto-generated method stub		
	}

	public void pack() {
		// TODO Auto-generated method stub	
	}

}

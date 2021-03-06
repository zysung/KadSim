package peersim;

/**
 * A Kademlia implementation for PeerSim extending the EDProtocol class.<br>
 * See the Kademlia bibliografy for more information about the protocol.
 *
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */

import java.math.BigInteger;
import java.util.*;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;
import peersim.transport.UnreliableTransport;

//__________________________________________________________________________________________________
public class KademliaProtocol implements Cloneable, EDProtocol {

	// VARIABLE PARAMETERS
	final String PAR_K = "K";
	final String PAR_ALPHA = "ALPHA";
	final String PAR_BITS = "BITS";

	private static final String PAR_TRANSPORT = "transport";
	private static String prefix = null;
	private UnreliableTransport transport;
	private int tid;
	private int kademliaid;

	/**
	 * allow to call the service initializer only once
	 */
	private static boolean _ALREADY_INSTALLED = false;

	/**
	 * nodeId of this pastry node
	 */
	public BigInteger nodeId;

	/**
	 * routing table of this pastry node
	 */
	public RoutingTable routingTable;

	/**
	 * trace message sent for timeout purpose
	 */
	private TreeMap<Long, Long> sentMsg;

	/**
	 * find operations set
	 */
	private LinkedHashMap<Long, FindOperation> findOp;

	/**
	 * Node store
	 */
	private TreeMap<BigInteger,Object> storeMap;

	/**
	 * send sth to store map <value,store times>
	 */
	private Map<String,Integer>  storeTimesMap;

	private boolean storeSucceedFlag = false;

	/**
	 * node store capacity
	 */
	private int storeCapacity;
	/**
	 * 存节点发来的存储容量，排序后再发STORE
	 */
	private Map<BigInteger,Integer> nodeSpace;

	private List<String> receivedVals;

	private List<String> findVals;

	/**
	 * Replicate this object by returning an identical copy.<br>
	 * It is called by the initializer and do not fill any particular field.
	 * 
	 * @return Object
	 */
	public Object clone() {
		KademliaProtocol dolly = new KademliaProtocol(KademliaProtocol.prefix);
		return dolly;
	}

	@Override
	public String toString() {
		return "KademliaProtocol{" +
				"PAR_K='" + PAR_K + '\'' +
				", PAR_ALPHA='" + PAR_ALPHA + '\'' +
				", PAR_BITS='" + PAR_BITS + '\'' +
				", transport=" + transport +
				", tid=" + tid +
				", kademliaid=" + kademliaid +
				", nodeId=" + nodeId +
				", routingTable=" + routingTable +
				", sentMsg=" + sentMsg +
				", findOp=" + findOp +
				", storeMap=" + storeMap +
				", storeCapacity=" + storeCapacity +
				'}';
	}

	/**
	 * Used only by the initializer when creating the prototype. Every other instance call CLONE to create the new object.
	 * 
	 * @param prefix
	 *            String
	 */
	public KademliaProtocol(String prefix) {
		this.nodeId = null; // empty nodeId
		KademliaProtocol.prefix = prefix;

		_init();

		routingTable = new RoutingTable();

		sentMsg = new TreeMap<Long, Long>();

		findOp = new LinkedHashMap<Long, FindOperation>();

		storeMap = new TreeMap<>();

		receivedVals = new ArrayList<>();

		findVals = new ArrayList<>();

		storeTimesMap = new HashMap<>();

		nodeSpace = new TreeMap<>();

		//给每个节点随机分配存储容量，为下面三个中之一
		int[] arr = {100,500,1000};
//		int[] arr = {500};
		int rand = (int)(Math.random() * arr.length);
		storeCapacity = arr[rand];

		tid = Configuration.getPid(prefix + "." + PAR_TRANSPORT);
	}

	/**
	 * This procedure is called only once and allow to inizialize the internal state of KademliaProtocol. Every node shares the
	 * same configuration, so it is sufficient to call this routine once.
	 */
	private void _init() {
		// execute once
		if (_ALREADY_INSTALLED)
			return;

		// read parameters
		KademliaCommonConfig.K = Configuration.getInt(prefix + "." + PAR_K, KademliaCommonConfig.K);
		KademliaCommonConfig.ALPHA = Configuration.getInt(prefix + "." + PAR_ALPHA, KademliaCommonConfig.ALPHA);
		KademliaCommonConfig.BITS = Configuration.getInt(prefix + "." + PAR_BITS, KademliaCommonConfig.BITS);

		_ALREADY_INSTALLED = true;
	}

	/**
	 * Search through the network the Node having a specific node Id, by performing binary search (we concern about the ordering
	 * of the network).
	 * 二分查找找某节点
	 * @param searchNodeId
	 *            BigInteger
	 * @return Node
	 */
	private Node nodeIdtoNode(BigInteger searchNodeId) {
		if (searchNodeId == null)
			return null;

		int inf = 0;
		int sup = Network.size() - 1;
		int m;

		while (inf <= sup) {
			m = (inf + sup) / 2;

			BigInteger mId = ((KademliaProtocol) Network.get(m).getProtocol(kademliaid)).nodeId;

			if (mId.equals(searchNodeId))
				return Network.get(m);

			if (mId.compareTo(searchNodeId) < 0)
				inf = m + 1;
			else
				sup = m - 1;
		}

		// perform a traditional search for more reliability (maybe the network is not ordered)
		BigInteger mId;
		for (int i = Network.size() - 1; i >= 0; i--) {
			mId = ((KademliaProtocol) Network.get(i).getProtocol(kademliaid)).nodeId;
			if (mId.equals(searchNodeId))
				return Network.get(i);
		}

		return null;
	}

	/**
	 * 收到route的resp后继续路由
	 * Perform the required operation upon receiving a message in response to a ROUTE message.<br>
	 * Update the find operation record with the closest set of neighbor received. Than, send as many ROUTE request I can
	 * (according to the ALPHA parameter).<br>
	 * If no closest neighbor available and no outstanding messages stop the find operation.
	 *
	 * @param m
	 *            Message
	 * @param myPid
	 *            the sender Pid
	 */
	private void route(Message m, int myPid) {
		// add message source to my routing table
		if (m.src != null) {
			routingTable.addNeighbour(m.src);//将发送resp的节点加到路由表
		}

		// get corresponding find operation (using the message field operationId)
		FindOperation fop = this.findOp.get(m.operationId);//拿到fop，各个node维护fop map，相当于一个传输过程，过程中各节点保持一致

		if (fop != null) {
			// save received neighbor in the closest Set of find operation
			try {
				fop.elaborateResponse((BigInteger[]) m.body);//m.body中节点已知的k个离目标最近节点，用这些节点更新fop的closeSet
			} catch (Exception ex) {
				fop.available_requests++;
			}

			while (fop.available_requests > 0) { // I can send a new find request还能再发路由请求

				// get an available neighbor
				BigInteger neighbour = fop.getNeighbour();

				if (neighbour != null) {
					// create a new request to send to neighbor
					Message request = new Message(Message.MSG_ROUTE);
					request.operationId = m.operationId;
					request.src = this.nodeId;
					request.dest = m.dest;

					// increment hop count
					fop.nrHops++;

					// send find request
					sendMessage(request, neighbour, myPid);
				} else if (fop.available_requests == KademliaCommonConfig.ALPHA) { // no new neighbor and no outstanding requests
					// search operation finished
					findOp.remove(fop.operationId);
					//随机生成的FIND_NODE消息
					if (fop.body.equals("Automatically Generated Traffic") && fop.closestSet.containsKey(fop.destNode)) {
						// update statistics
						long timeInterval = (CommonState.getTime()) - (fop.timestamp);
						KademliaObserver.timeStore.add(timeInterval);
						KademliaObserver.hopStore.add(fop.nrHops);
						KademliaObserver.msg_deliv.add(1);
					}else if(fop.body instanceof  StoreFile){  //add store to closeset
						for (BigInteger node: fop.closestSet.keySet()) {
							Message storeSpaceReqMsg = new Message(Message.MSG_STORE_SPACE_REQ,fop.body);
							storeSpaceReqMsg.src =  this.nodeId;
							storeSpaceReqMsg.dest = node;
							storeSpaceReqMsg.operationId = m.operationId;
							sendMessage(storeSpaceReqMsg,node,myPid);
//							System.out.println("send space ask msg to node:"+node);
						}
					}else if(fop.body instanceof String){
						for (BigInteger node: fop.closestSet.keySet()
								) {
							Message findValMsg = new Message(Message.MSG_FINDVALUE,fop.body);
							findValMsg.src = this.nodeId;
							findValMsg.dest = node;
							findValMsg.operationId = m.operationId;
							sendMessage(findValMsg,node,myPid);
						}
					}

					return;

				} else { // no neighbor available but exists oustanding request to wait
					return;
				}
			}
		} else {
			System.err.println("There has been some error in the protocol");
		}
	}

	/**
	 * Response to a route request.<br>
	 * Find the ALPHA closest node consulting the k-buckets and return them to the sender.
	 *返回本节点已知的K个离目标节点最近的节点
	 * @param m
	 *            Message
	 * @param myPid
	 *            the sender Pid
	 */
	private void routeResponse(Message m, int myPid) {
		// get the ALPHA closest node to destNode
		BigInteger[] neighbours = this.routingTable.getNeighbours(m.dest, m.src);

		// create a response message containing the neighbors (with the same id of the request)
		Message response = new Message(Message.MSG_RESPONSE, neighbours);//将本节点已知的k个最近节点返回给src节点
		response.operationId = m.operationId;
		response.dest = m.dest;
		response.src = this.nodeId;
		response.ackId = m.id; // set ACK number

		// send back the neighbours to the source of the message
		sendMessage(response, m.src, myPid);
	}

	/**
	 * Start a find node operation.
	 * Find the ALPHA closest node and send find request to them.
	 *
	 * @param m
	 *            Message received (contains the node to find)
	 * @param myPid
	 *            the sender Pid
	 */
	private void find(Message m, int myPid) {

		KademliaObserver.find_op.add(1);

		// create find operation and add to operations array
		FindOperation fop = new FindOperation(m.dest, m.timestamp);
		fop.body = m.body;
		findOp.put(fop.operationId, fop);


		// get the ALPHA closest node to srcNode and add to find operation
		BigInteger[] neighbours = this.routingTable.getNeighbours(m.dest, this.nodeId);
		fop.elaborateResponse(neighbours);
		fop.available_requests = KademliaCommonConfig.ALPHA;

		// set message operation id，将信息转发
		m.operationId = fop.operationId;
		m.type = Message.MSG_ROUTE;
		m.src = this.nodeId;

		// send ALPHA messages
		for (int i = 0; i < KademliaCommonConfig.ALPHA; i++) {
			BigInteger nextNode = fop.getNeighbour(); //get the first neighbor in closest set which has not been already queried
			if (nextNode != null) {
				sendMessage(m.copy(), nextNode, myPid);
				fop.nrHops++;
			}
		}

	}


	private void store(Message m,int myPid){
		StoreFile sf = (StoreFile) m.body;
		boolean storedSucceed = false;
		if(this.storeCapacity>=sf.getSize()) {
			this.storeMap.put(sf.getKey(), sf.getValue());
			System.out.println("Node:" + this.nodeId+"("+this.storeCapacity+"-"+sf.getSize()+")" + " storing kv data:" + sf.toString());
			this.storeCapacity -= sf.getSize();
//			StoreMessageGenerator.generateStoreVals.add((String)((StoreFile)m.body).getValue());

			KademliaObserver.real_store_operation.add(1);
			storedSucceed = true;
		}else {
			System.out.println("Node:" + this.nodeId+ ":" +this.storeCapacity+ " can't storing kv data:" + sf.toString());
		}
		String respBody = sf.getValue() + "-"+storedSucceed;
		Message storeRespMsg = new Message(Message.MSG_STORE_RESP,respBody);
		storeRespMsg.src = this.nodeId;
		storeRespMsg.dest = m.src;
		storeRespMsg.operationId = m.operationId;
		KademliaObserver.sendstore_resp.add(1);
		sendMessage(storeRespMsg,m.src,myPid);

	}

	private void getStoreResp(Message m,int myPid){
		String resp = (String)m.body;
		String value = resp.split("-")[0];
		boolean isSucceed =  Boolean.parseBoolean(resp.split("-")[1]);
//		System.out.println(this.storeTimesMap.keySet().contains(value) );
		if(this.storeTimesMap.keySet().contains(value) && isSucceed){
			int times = storeTimesMap.get(value);
//			System.out.println("times:"+times + "  flag:"+storeSucceedFlag);
			storeTimesMap.put(value,++times);
			if(times > 0 && !storeSucceedFlag){
				storeSucceedFlag = true;
				KademliaObserver.stored_msg.add(1);
			}
		}
	}

	private void returnSpace(Message m,int myPid){
		StoreFile sf = new StoreFile(((StoreFile) m.body).getKey(),((StoreFile) m.body).getValue());
		sf.setSize(((StoreFile) m.body).getSize());
		sf.setStoreNodeRemainSize(this.storeCapacity);
		Message spaceRespMsg = new Message(Message.MSG_STORE_SPACE_RESP,sf);
		spaceRespMsg.src = this.nodeId;
		spaceRespMsg.dest = m.src;
		spaceRespMsg.operationId = m.operationId;
		sendMessage(spaceRespMsg,m.src,myPid);
		System.out.println("node:"+this.nodeId+"return space:"+sf.getStoreNodeRemainSize());
	}

	private void sortBySpaceAndSendStore(Message m,int myPid){
		//FindOperation fop = this.findOp.get(m.operationId);
		System.out.println("node:"+this.nodeId+"receive space:"+((StoreFile)m.body).getStoreNodeRemainSize()+" from "+m.src);
		this.nodeSpace.put(m.src,((StoreFile)m.body).getStoreNodeRemainSize());

		if(this.nodeSpace.size()>=KademliaCommonConfig.K){
			List<Map.Entry<BigInteger, Integer>> list = new ArrayList<>(nodeSpace.entrySet());
			// 通过比较器来实现排序
			Collections.sort(list, (o1, o2) -> {
				// 降序排序
				return o2.getValue().compareTo(o1.getValue());
			});
//			for (Map.Entry<BigInteger, Integer> mapping : list) {
//				System.out.println(mapping.getKey() + ":" + mapping.getValue());
//			}
			int i = 3;
			for (Map.Entry<BigInteger, Integer> nodeMap : list) {
				Message storeMsg = new Message(Message.MSG_STORE, m.body);
				storeMsg.src = this.nodeId;
				storeMsg.dest = nodeMap.getKey();
				storeMsg.operationId = m.operationId;
				sendMessage(storeMsg, nodeMap.getKey(), myPid);
				if (--i == 0) break;
			}
			this.nodeSpace.clear();
		}
	}


	private void getValue(Message m,int myPid){
		String value = (String)m.body;
		for (Object val:this.storeMap.values()
			 ) {
			if(val.equals(value)){
				Message returnValMsg = new Message(Message.MSG_RETURNVALUE,value);
				returnValMsg.src = this.nodeId;
				returnValMsg.dest = m.src;
				returnValMsg.operationId = m.operationId;
				returnValMsg.body = val;
				System.out.println("node:"+nodeId+" return value "+val+" to node:"+m.src);
				sendMessage(returnValMsg,m.src,myPid);
				break;
			}
		}
	}

	private void receiveVal(Message m,int myPid){
		String receVal = (String)m.body;
		if(!receivedVals.contains(receVal)){
			receivedVals.add(receVal);
			KademliaObserver.findVal_success.add(1);
			System.err.println("node "+this.nodeId + "'s findVals:"+this.findVals);
			System.err.println("node "+this.nodeId + "'s receiveVals:"+this.receivedVals);
		}
	}


	/**
	 * send a message with current transport layer and starting the timeout timer (which is an event) if the message is a request
	 *
	 * @param m
	 *            the message to send
	 * @param destId
	 *            the Id of the destination node
	 * @param myPid
	 *            the sender Pid
	 */
	public void sendMessage(Message m, BigInteger destId, int myPid) {
		// add destination to routing table
		this.routingTable.addNeighbour(destId);

		Node src = nodeIdtoNode(this.nodeId);
		Node dest = nodeIdtoNode(destId);

		transport = (UnreliableTransport) (Network.prototype).getProtocol(tid);
		transport.send(src, dest, m, kademliaid);

		if (m.getType() == Message.MSG_ROUTE) { // is a request
			Timeout t = new Timeout(destId, m.id, m.operationId);
			long latency = transport.getLatency(src, dest);
			// add to sent msg
			this.sentMsg.put(m.id, m.timestamp);
			EDSimulator.add(4 * latency, t, src, myPid); // set delay = 2*RTT(往返时间)
		}
	}

	/**
	 * manage the peersim receiving of the events
	 *
	 * @param myNode
	 *            Node
	 * @param myPid
	 *            int
	 * @param event
	 *            Object
	 */
	public void processEvent(Node myNode, int myPid, Object event) {

		// Parse message content Activate the correct event manager fot the particular event
		this.kademliaid = myPid;

		Message m;

//		if((((KademliaProtocol) this).getNodeId().equals(new BigInteger("1436294760649089404056870643165375864014011356023")))){
//			KademliaProtocol kp = ((KademliaProtocol) (myNode.getProtocol(myPid)));
//			System.out.println(Message.messageTypetoString(((SimpleEvent) event).getType()));
//		}

		switch (((SimpleEvent) event).getType()) {

			case Message.MSG_RESPONSE:
				m = (Message) event;
				sentMsg.remove(m.ackId);
				route(m, myPid);
				break;

			case Message.MSG_FINDNODE:
				m = (Message) event;
				find(m, myPid);
				break;

			case Message.MSG_ROUTE:
				m = (Message) event;
				routeResponse(m, myPid);
				break;

			case Message.MSG_EMPTY:
				// TO DO
				break;

			case Message.MSG_STORE:
				m = (Message)event;
				store(m,myPid);
				break;

			case Message.MSG_STORE_REQUEST:
				m = (Message)event;
				System.out.println("This node:" + this.getNodeId()+"get kv to store:"+m.body);
				System.out.println("the generateStore:"+StoreMessageGenerator.generateStoreVals.size());
				find(m,myPid);
				this.storeTimesMap.put((String)((StoreFile) m.body).getValue(),0);

				KademliaObserver.sendtostore_msg.add(1);
				break;

			case Message.MSG_STORE_RESP:
				m = (Message)event;
				getStoreResp(m,myPid);
				break;

			case Message.MSG_STORE_SPACE_REQ:
				m = (Message)event;
				returnSpace(m,myPid);
				break;

			case Message.MSG_STORE_SPACE_RESP:
				m = (Message)event;
				sortBySpaceAndSendStore(m,myPid);
				break;

//			case Message.MSG_FINDVALUE_REQ:
//				m = (Message)event;
//				System.out.println("This node:" + this.getNodeId()+"finding value:"+m.body);
//				if(!findVals.contains((String)m.body)) {
//					findVals.add((String) m.body);
//					find(m, myPid);
//					System.err.println("node:"+this.nodeId+"'s findVals:"+this.findVals);
//					KademliaObserver.findVal_times.add(1);
//				}else {
//					System.out.println("This node already find this value:" + m.body);
//				}
//				break;

//			case Message.MSG_FINDVALUE:
//				m = (Message)event;
//				getValue(m,myPid);
//				break;

//			case Message.MSG_RETURNVALUE:
//				m = (Message)event;
//				receiveVal(m,myPid);
//				break;

			case Timeout.TIMEOUT: // timeout
				Timeout t = (Timeout) event;
				if (sentMsg.containsKey(t.msgID)) { // the response msg isn't arrived
					// remove form sentMsg
					sentMsg.remove(t.msgID);
					// remove node from my routing table
					this.routingTable.removeNeighbour(t.node);
					// remove from closestSet of find operation
					this.findOp.get(t.opID).closestSet.remove(t.node);
					// try another node
					Message m1 = new Message();
					m1.operationId = t.opID;
					m1.src = nodeId;
					m1.dest = this.findOp.get(t.opID).destNode;
					this.route(m1, myPid);
				}
				break;

		}

	}

	/**
	 * set the current NodeId
	 * 
	 * @param tmp
	 *            BigInteger
	 */
	public void setNodeId(BigInteger tmp) {
		this.nodeId = tmp;
		this.routingTable.nodeId = tmp;
	}

	public BigInteger getNodeId() {
		return nodeId;
	}
}

package peersim;

import sun.misc.resources.Messages_fr;

import java.math.BigInteger;

/**
 * 
 * Message class provide all functionalities to magage the various messages, principally LOOKUP messages (messages from
 * application level sender destinated to another application level).<br>
 * 
 * Types Of messages:<br>
 * (application messages)<BR>
 * - MSG_LOOKUP: indicates that the body Object contains information to application level of the recipient<BR>
 * <br>
 * (service internal protocol messages)<br>
 * - MSG_JOINREQUEST: message containing a join request of a node, the message is passed between many pastry nodes according to
 * the protocol<br>
 * - MSG_JOINREPLY: according to protocol, the body transport information related to a join reply message <br>
 * - MSG_LSPROBEREQUEST:according to protocol, the body transport information related to a probe request message <br>
 * - MSG_LSPROBEREPLY: not used in the current implementation<br>
 * - MSG_SERVICEPOLL: internal message used to provide cyclic cleaning service of dead nodes<br>
 * 
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
// ______________________________________________________________________________________
public class Message extends SimpleEvent {

	/**
	 * internal generator for unique message IDs
	 */
	private static long ID_GENERATOR = 0;

	/**
	 * Message Type: PING (used to verify that a node is still alive)
	 */
	public static final int MSG_EMPTY = 0;

	/**
	 * Message Type: STORE (Stores a (key, value) pair in one node)
	 */
	public static final int MSG_STORE = 1;

	/**
	 * Message Type: FINDNODE (message regarding node find)
	 */
	public static final int MSG_FINDNODE = 2;

	/**
	 * Message Type: FINDVALUE (message regarding value find)
	 */
	public static final int MSG_ROUTE = 3;

	/**
	 * Message Type: RESPONSE (response message to a findvalue or findnode)
	 */
	public static final int MSG_RESPONSE = 4;

	/**
	 * Message Type: STORE_REQUEST(request some node to find closest node to store a file)_
	 */
	public static final int MSG_STORE_REQUEST = 5;

	/**
	 * Message Type: MSG_FINDVALUE
	 */
	public static final int  MSG_FINDVALUE_REQ= 6;

	public static final int MSG_FINDVALUE = 7;
	// ______________________________________________________________________________________________
	/**
	 * This Object contains the body of the message, no matter what it contains
	 */
	public Object body = null;

	/**
	 * ID of the message. this is automatically generated probe, and should not change
	 */
	public long id;

	/**
	 * ACK number of the message. This is in the response message.
	 */
	public long ackId;

	/**
	 * Id of the search operation
	 */
	public long operationId;

	/**
	 * Recipient address of the message
	 */
	public BigInteger dest;

	/**
	 * Source address of the message: has to be filled at application level
	 */
	public BigInteger src;

	/**
	 * Available to count the number of hops the message did.
	 */
	protected int nrHops = 0;

	// ______________________________________________________________________________________________
	/**
	 * Creates an empty message by using default values (message type = MSG_EMPTY and <code>new String("")</code> value for the
	 * body of the message)
	 */
	public Message() {
		this(MSG_EMPTY, "");
	}

	/**
	 * Create a message with specific type and empty body
	 * 
	 * @param messageType
	 *            int type of the message
	 */
	public Message(int messageType) {
		this(messageType, "");
	}

	// ______________________________________________________________________________________________
	/**
	 * Creates a message with specific type and body
	 * 
	 * @param messageType
	 *            int type of the message
	 * @param body
	 *            Object body to assign (shallow copy)
	 */
	public Message(int messageType, Object body) {
		super(messageType);
		this.id = (ID_GENERATOR++);
		this.body = body;
	}

	// ______________________________________________________________________________________________
	/**
	 * Encapsulates the creation of a find value request
	 * 
	 * @param body
	 *            Object
	 * @return Message
	 */
	/*public static final Message makeFindValue(Object body) {
		return new Message(MSG_FINDVALUE, body);
	}*/

	// ______________________________________________________________________________________________
	/**
	 * Encapsulates the creation of a find node request
	 * 
	 * @param body
	 *            Object
	 * @return Message
	 */
	public static final Message makeFindNode(Object body) {
		return new Message(MSG_FINDNODE, body);
	}

	/**
	 * make a store request
	 * @param body
	 * @return
	 */
	public static final Message makeStoreReq(Object body){
		return new Message(MSG_STORE_REQUEST,body);
	}

	/**
	 * make a findvalue request
	 * @param body
	 * @return
	 */
	public static final Message makeFindValue(Object  body){
		return new Message(MSG_FINDVALUE,body);
	}

	// ______________________________________________________________________________________________
	/**
	 * Encapsulates the creation of a find value request
	 * 
	 * @return Message
	 */
	/*public static final Message makeJoinRequest(Object body) {
		return new Message(MSG_FINDVALUE, body);
	}*/

	// ______________________________________________________________________________________________
	public String toString() {
		String s = "[ID=" + id + "][SRC="+ src + "]" +"][DEST=" + dest + "]";
		return s + "[Type=" + messageTypetoString() + "] BODY=(...)";
	}

	// ______________________________________________________________________________________________
	public Message copy() {
		Message dolly = new Message();
		dolly.type = this.type;
		dolly.src = this.src;
		dolly.dest = this.dest;
		dolly.operationId = this.operationId;
		dolly.body = this.body; // deep cloning?

		return dolly;
	}

	// ______________________________________________________________________________________________
	public String messageTypetoString() {
		switch (type) {
			case MSG_EMPTY:
				return "MSG_EMPTY";
			case MSG_STORE:
				return "MSG_STORE";
			case MSG_FINDNODE:
				return "MSG_FINDNODE";
			case MSG_ROUTE:
				return "MSG_FINDVALUE";
			default:
				return "UNKNOW:" + type;
		}
	}

	public static  String messageTypetoString(int type) {
		switch (type) {
			case MSG_EMPTY:
				return "MSG_EMPTY";
			case MSG_STORE:
				return "MSG_STORE";
			case MSG_FINDNODE:
				return "MSG_FINDNODE";
			case MSG_ROUTE:
				return "MSG_FINDVALUE";
			case  MSG_RESPONSE:
				return  "MSG_ROUTE_RESPONSE";
			default:
				return "UNKNOW:" + type;
		}
	}


}

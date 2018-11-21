package peersim;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

/**
 * This control generates random search traffic from nodes to random destination node.
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */

// ______________________________________________________________________________________________
public class FindValueMessageGenerator implements Control {

	// ______________________________________________________________________________________________
	/**
	 * MSPastry Protocol to act
	 */
	private final static String PAR_PROT = "protocol";

	/**
	 * MSPastry Protocol ID to act
	 */
	private final int pid;


	// ______________________________________________________________________________________________
	public FindValueMessageGenerator(String prefix) {
		pid = Configuration.getPid(prefix + "." + PAR_PROT);
	}

    /**
     * 生成key为随机160bit,value为一个空obj的模拟StoreFile
	 * @return
     */
	//______________________________________________________________________________________________
	private Message generateFindValueMessage(){
		if(StoreMessageGenerator.generateStoreVals != null && !StoreMessageGenerator.generateStoreVals.isEmpty()) {
			String value = StoreMessageGenerator.generateStoreVals.get(new Random().nextInt(StoreMessageGenerator.generateStoreVals.size()));
			BigInteger key = null;
			try {
				key = new BigInteger(SHA1.shaEncode(value), 16);
			} catch (Exception e) {
				e.printStackTrace();
			}

			Message m = Message.makeStoreReq(value);
			m.timestamp = CommonState.getTime();
			m.dest = key;

			return m;
		}
		return null;
	}



	// ______________________________________________________________________________________________
	/**
	 * every call of this control generates and send a random find node message
	 * 
	 * @return boolean
	 */
	public boolean execute() {
		Node start;
		do {
			start = Network.get(CommonState.r.nextInt(Network.size()));
		} while ((start == null) || (!start.isUp()));

		// send message
		if(StoreMessageGenerator.generateStoreVals != null && !StoreMessageGenerator.generateStoreVals.isEmpty()) {
			EDSimulator.add(0, generateFindValueMessage(), start, pid);
		}

		return false;
	}

	// ______________________________________________________________________________________________

} // End of class
// ______________________________________________________________________________________________

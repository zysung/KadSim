package peersim;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.omg.PortableInterceptor.INACTIVE;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.util.IncrementalStats;

/**
 * This class implements a simple observer of search time and hop average in finding a node in the network
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class KademliaObserver implements Control {

	/**
	 * keep statistics of the number of hops of every message delivered.
	 */
	public static IncrementalStats hopStore = new IncrementalStats();

	/**
	 * keep statistics of the time every message delivered.
	 */
	public static IncrementalStats timeStore = new IncrementalStats();

	/**
	 * keep statistic of number of message delivered
	 */
	public static IncrementalStats msg_deliv = new IncrementalStats();

	/**
	 * keep statistic of number of find operation
	 */
	public static IncrementalStats find_op = new IncrementalStats();

	/**
	 * keep statistic of number of  successful store message,表示成功存储的kv个数(ps:成功存储到一个节点就算成功存储)
	 */
	public static IncrementalStats stored_msg = new IncrementalStats();

	public static IncrementalStats real_store_operation = new IncrementalStats();


	/**
	 * keep statistic of number of  failed store message,表示存储失败的kv个数
	 */
	public static IncrementalStats sendtostore_msg = new IncrementalStats();

	public static IncrementalStats sendstore_resp = new IncrementalStats();
	/**
	 * 过载节点的个数
	 */
	public static IncrementalStats overloadNode = new IncrementalStats();
	/**
	 * keep statistic of number of  find value success,表示成功find value的次数
	 */
	public static IncrementalStats findVal_success  = new IncrementalStats();

	/**
	 * 发起find value的次数
	 */
	public static IncrementalStats findVal_times = new IncrementalStats();



	/** Parameter of the protocol we want to observe */
	private static final String PAR_PROT = "protocol";

	/** Protocol id */
	private int pid;

	/** Prefix to be printed in output */
	private String prefix;

	public KademliaObserver(String prefix) {
		this.prefix = prefix;
		pid = Configuration.getPid(prefix + "." + PAR_PROT);
	}

	/**
	 * print the statistical snapshot of the current situation
	 * 
	 * @return boolean always false
	 */
	public boolean execute() {
		// get the real network size
		int sz = Network.size();
		for (int i = 0; i < Network.size(); i++)
			if (!Network.get(i).isUp())
				sz--;

		String s = String.format("[time=%d]:[N=%d current nodes UP] [D=%f msg deliv] [%f min h] [%f average h] [%f max h] [%d min l] [%d msec average l] [%d max l] [%d findop sum] [%d sendstore_resp sum]  [%d storedMsg sum]  [%d sendtostore_msg sum] [%d findValueSuccess sum] [%d findValueTimes][%d realStoreOperation]",
				CommonState.getTime(), sz, msg_deliv.getSum(),hopStore.getMin(), hopStore.getAverage(), hopStore.getMax(), (int) timeStore.getMin(), (int) timeStore.getAverage(), (int) timeStore.getMax(),(int)find_op.getSum(),(int)sendstore_resp.getSum(),(int)stored_msg.getSum(),(int)sendtostore_msg.getSum(),(int)findVal_success.getSum(),(int)findVal_times.getSum(),(int)real_store_operation.getSum());

		if (CommonState.getTime() == 3600000) {
			// create hop file
			try {
				File f = new File("H:/simulazioni/hopcountNEW.dat"); // " + sz + "
				f.createNewFile();
				BufferedWriter out = new BufferedWriter(new FileWriter(f, true));
				out.write(String.valueOf(hopStore.getAverage()).replace(".", ",") + ";\n");
				out.close();
			} catch (IOException e) {
			}
			// create latency file
			try {
				File f = new File("H:/simulazioni/latencyNEW.dat");
				f.createNewFile();
				BufferedWriter out = new BufferedWriter(new FileWriter(f, true));
				out.write(String.valueOf(timeStore.getAverage()).replace(".", ",") + ";\n");
				out.close();
			} catch (IOException e) {
			}

		}

		System.err.println(s);

		return false;
	}
}

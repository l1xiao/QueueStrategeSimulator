package project;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Queue;

public class Processor {
	private static ServerSocket server;
	private static Socket client;
	static int PORT1 = 8080;
	static int timePort = 8081;
	static Integer endTime = Setting.endTime;
	static Integer currentTime = -1;
	static Queue<Integer[]> queue = new LinkedList<>();
	static Queue<Integer[]>[] queues;
	static int count = 0;
	static final int priorities = 5;
	boolean stoplisten = false;
	private static int model = 0;
	private Recorder record = new Recorder(200);

	// sort by value
	public class ArrayIndexComparator implements Comparator<Integer> {
		private final Integer[] array;

		public ArrayIndexComparator(Integer[] array) {
			this.array = array;
		}

		public Integer[] createIndexArray() {
			Integer[] indexes = new Integer[array.length];
			for (int i = 0; i < array.length; i++) {
				indexes[i] = i;
			}
			return indexes;
		}

		@Override
		public int compare(Integer index1, Integer index2) {
			// Autounbox from Integer to int to use as array indexes
			return array[index1].compareTo(array[index2]);
		}
	}

	// statistic for incoming data
	private class Recorder {
		// window size
		private int n;
		// counter records the number of five priorities query
		private Integer[] counter;
		private boolean flag;
		private LinkedList<Integer[]> _queue;
		private Integer[] lastRank = { 0, 1, 2, 3, 4 };

		Recorder(int n) {
			this.n = n;
			this._queue = new LinkedList<Integer[]>();
			this.counter = new Integer[priorities];
			for (int i = 0; i < priorities; i++) {
				this.counter[i] = 0;
			}
			this.flag = false;
		}

		// return false after record if rank not change
		public boolean record(Integer[] data) {
			_queue.add(data);
			System.out.println(counter[0]);
			counter[data[1] - 1]++;
			Integer[] last;
			// queue if full
			if (_queue.size() > n) {
				last = _queue.poll();
				counter[last[1] - 1]--;
			}
			ArrayIndexComparator comparator = new ArrayIndexComparator(counter);
			Integer[] indexes = comparator.createIndexArray();
			Arrays.sort(indexes, comparator);
			if (Arrays.equals(indexes, lastRank)) {
				return false;
			} else {
				return true;
			}
		}

		public Integer[] getCount() {
			return lastRank;
		}
		
		public Integer[] getStatistic() {
			return counter;
		}
		
		public int size() {
			return _queue.size();
		}
	}

	public Processor(int model) throws Exception {
		Processor.model = model;

		// buffer queue array
		queues = new Queue[5];
		for (int i = 0; i < 5; i++) {
			queues[i] = new LinkedList<>();
		}

		server = new ServerSocket(PORT1);
		// add data into queue
		listen();
		// consume data from queue
		process();
	}

	public int getTime(int request) throws IOException, Exception {
		// when need time, new a socket
		client = new Socket("localhost", timePort);
		OutputStream os = client.getOutputStream();

		ObjectOutputStream oos = new ObjectOutputStream(os);
		// clientId, requestTime
		Integer[] data = { 0, request };
		System.out.print("request:   {");
		for (Integer integer : data) {
			System.out.print(integer + " ");
		}
		oos.writeObject(data);
		InputStream is = client.getInputStream();
		ObjectInputStream ois = new ObjectInputStream(is);
		Integer time = (Integer) ois.readObject();
		System.out.println("}\tserver time:" + time);
		oos.close();
		os.close();
		ois.close();
		is.close();
		client.close();
		return time;
	}

	// process thread
	private void process() throws Exception {

		System.out.println("开始处理");
		int fail = 0;
		File f = new File("./result.txt");
		FileOutputStream fop = new FileOutputStream(f);
		OutputStreamWriter writer = new OutputStreamWriter(fop, "UTF-8");
		while (currentTime < endTime) {

			if (!queue.isEmpty()) {
				if (queue.peek()[4] + currentTime > endTime) {
					getTime(endTime + 1);
					System.out.println("结束了");
					// TODO 中断正在阻塞的socket
					break;
				}
				System.out.println("队列不空");
				System.out.println("detail:" + queue.peek()[0]
						+ "th query\tpriority:" + queue.peek()[1] + "\ttol:"
						+ queue.peek()[2] + "\tcosttime:" + queue.peek()[4]
						+ "\tsendtime:" + queue.peek()[3] + "\tcurrentime:"
						+ currentTime + "\n");
				// fail to process send time + tolerance time < currentTime
				if (queue.peek()[2] + queue.peek()[3] < currentTime) {
					System.out.println("this request is time out, currentTime"
							+ currentTime);
					for (Integer integer : queue.peek()) {
						System.out.print(integer + " ");
					}
					System.out.println();
					fail = fail + getFail(queue.peek()[1]);
					writer.append("fail:" + fail + "\tdetail:"
							+ queue.peek()[0] + "th query\tpriority:"
							+ queue.peek()[1] + "\ttol:" + queue.peek()[2]
							+ "\tcosttime:" + queue.peek()[4] + "\tsendtime:"
							+ queue.peek()[3] + "\tcurrentime:" + currentTime
							+ "\n");
					queue.poll();
					// if empty, tell time server
					if (queue.isEmpty()) {
						currentTime = getTime(-1);
					}
					continue;
				}
				System.out.println("当前队列大小：" + queue.size());
				// for debug
				// if (queue.size() == 1) {
				// System.out.println("detail:" + queue.peek()[0]
				// + "th query\tpriority:" + queue.peek()[1]
				// + "\ttol:" + queue.peek()[2] + "\tcosttime:"
				// + queue.peek()[4] + "\tsendtime:" + queue.peek()[3]
				// + "\tcurrentime:" + currentTime + "\n");
				// }
				// generate data: [id, priority, tolerance, generateTime];
				// when all task finished, currentTime will be setted to -1,
				// when
				// new item enqueue, currenTime should be send time.
				if (currentTime == -1) {
					currentTime = queue.peek()[3];
				}
				int serverTime = getTime(queue.peek()[4] + currentTime);
				// if serverTime less than cost time + currentTime, do nothing
				if (serverTime < queue.peek()[4] + currentTime) {
					// do nothing

					// if serverTime >= endTime, mean all tasks whose send time
					// >= serverTime should be aborted.
					if (serverTime >= endTime) {
						currentTime = serverTime;
						break;
					}
				} else {
					System.out.println("消费数据！！！！！！");
					// consume data

					writer.append("success\tdetail:" + queue.peek()[0]
							+ "th query\tpriority:" + queue.peek()[1]
							+ "\ttol:" + queue.peek()[2] + "\tcosttime:"
							+ queue.peek()[4] + "\tsendtime:" + queue.peek()[3]
							+ "\tfinish:" + serverTime + "\n");
					System.out.println("success\tdetail:" + queue.peek()[0]
							+ "th query\tpriority:" + queue.peek()[1]
							+ "\ttol:" + queue.peek()[2] + "\tcosttime:"
							+ queue.peek()[4] + "\tsendtime:" + queue.peek()[3]
							+ "\tfinish:" + serverTime + "\n");
					System.out.println("消费数据," + "server time:" + serverTime
							+ " wantTime:" + (queue.peek()[4] + currentTime));
					currentTime = serverTime;
					queue.poll();
					if (queue.isEmpty()) {
						System.out.println("队列空了");
						currentTime = getTime(-1);
					}
				}
			} else {

			}
		}

		writer.append("\ntotal_fail:" + fail);
		System.out.println("=== fail:" + fail + " ===");
		try {
			// FileOutputStream构造函数中的第二个参数true表示以追加形式写文件
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream("calculate.txt", true), "utf8"));
			out.write("model" + model + "\t" + fail + "\n");
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		stoplisten = true;
		writer.close();
		fop.close();
	}

	private int getFail(int priority) {
		// return priority;
		return (int) Math.pow(2, priority);
	}

	public synchronized void enqueue(Integer[] data) {
		// Model:FIFO
		if (model == 0) {
			queue.add(data);
			System.out.println(data[0] + "th 加入队列");
		} 
		// Model:WQF
		else if (model == 1) {
			// judge the priority of the data
			int priority = data[1];
			queues[priority - 1].add(data);
			// make sure dequeue one item
			System.out.println("count:" + count + "\tqueue size:"
					+ queue.size());
			System.out.println("id:" + queues[priority - 1].peek()[0]
					+ "\tpriority:" + priority);
			// default rank
			Integer[] rank = { 0, 1, 2, 3, 4 };
			Integer[][] getDistribution = setDistribution(rank, 0);
			distribute(getDistribution[0], getDistribution[1][0]);
			System.out.println("enqueue over, queue size:" + queue.size());
			System.out.println("id:" + queue.peek());
		} 
		// Model:Dynamic Weight Queue
		else if (model == 2) {
			// put data into corresponded queue
			int priority = data[1];
			queues[priority - 1].add(data);
			record.record(data);
			Integer[] current = record.getCount();
			// set option
			Integer[][] distribution = setDistribution(current, 1);
			distribute(distribution[0], distribution[1][0]);
		}
	}

	// return a 2D array, {0:distribution, 1:length};
	private Integer[][] setDistribution(Integer[] rank, int option) {
		Integer[] distribution;
		int length = 0;
		Integer[][] result = new Integer[2][];
		// auto adjust according to rank
		if (option == 0) {
			Integer[] choice = new Integer[15];
			int count = 0;
			for (int i = 0; i < rank.length; i++) {
				for (int j = 0; j < rank.length - i; j++) {
					choice[count++] = rank[i];
				}
			}
			distribution = choice;
			length = distribution.length;
		} 
		// change weight according to the statistic
		else if (option == 1) {
			// Probability array
			Integer[] choice = new Integer[1000];
			Arrays.fill(choice, 0);
			// get rank with counter
			Integer[] counter = record.getStatistic();
			// set weight
			int j = 0;
			double[] coefficient = {0.5, 0.8, 1.1, 1.5, 2};
			for (int i = 0; i < priorities; i++) {
				double p = counter[i] / (double)record.size() * coefficient[i];
				for (; j < p * 100; j++) {
					choice[j] = i;
				}
			}
			distribution = choice;
			length = j;
		}
		// default case
		else {
			Integer[] choice = { 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 3, 4 };
			distribution = choice;
			length = distribution.length;
		}
		result[0] = distribution;
		result[1] = new Integer[1];
		result[1][0] = length;
		return result;
	}

	private void distribute(Integer[] distiburion, int length) {
		// TODO count should be generated randomly, not a constant.	
		int index = Setting.randomno.nextInt(1000) % length;
		// corresponded queue is not empty
		if (!queues[distiburion[index]].isEmpty()) {
			System.out.println("not empty");
			queue.add(queues[distiburion[index]].poll());
		}
		// current corresponded queue is empty, try to find a not empty queue
		else {
			for (int i = queues.length - 1; i > 0; i--) {
				if (!queues[i].isEmpty()) {
					queue.add(queues[i].poll());
					System.out.println("i queue:" + i);
					break;
				}
			}
		}
	}

	// listen thread
	public void listen() {
		Thread task = new Thread() {
			public void run() {
				while (!stoplisten) {
					System.out.println("start to listen...");
					try {

						Socket socket = server.accept();
						// System.out.println("accept");
						System.out.println("set connection with generator...");
						InputStream is = socket.getInputStream();
						ObjectInputStream ois = new ObjectInputStream(is);
						Integer[] input = (Integer[]) ois.readObject();
						System.out.println("query_id:" + input[0]);
						count++;
						enqueue(input);
					} catch (IOException | ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
			}
		};
		task.start();
	}

	public static void main(String[] args) throws Exception {
		@SuppressWarnings("unused")
		Processor processor = new Processor(Setting.model);
	}
}

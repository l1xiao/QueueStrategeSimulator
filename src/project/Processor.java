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
import java.util.LinkedList;
import java.util.Queue;

public class Processor {
	private static ServerSocket server;
	private static Socket client;
	static int PORT1 = 8080;
	static int timePort = 8081;
	static Integer endTime = 1000;
	static Integer currentTime = -1;
	static Queue<Integer[]> queue = new LinkedList<>();
	static Queue<Integer[]>[] queues;
	static int count = 0;
	boolean stoplisten = false;
	private static int model = 0;


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
					break; 
				}
				System.out.println("队列不空");
				 System.out.println("detail:" + queue.peek()[0]
				 + "th query\tpriority:" + queue.peek()[1]
				 + "\ttol:" + queue.peek()[2] + "\tcosttime:"
				 + queue.peek()[4] + "\tsendtime:" + queue.peek()[3]
				 + "\tcurrentime:" + currentTime + "\n");
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
//				 System.out.println("detail:" + queue.peek()[0]
//				 + "th query\tpriority:" + queue.peek()[1]
//				 + "\ttol:" + queue.peek()[2] + "\tcosttime:"
//				 + queue.peek()[4] + "\tsendtime:" + queue.peek()[3]
//				 + "\tcurrentime:" + currentTime + "\n");
				// }
				// generate data: [id, priority, tolerance, generateTime];
				// when all task finished, currentTime will be setted to 1, when
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
				System.out.println("--队列空了");
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
//		return priority;
		return (int) Math.pow(2, priority);
	}

	public synchronized static void enqueue(Integer[] data) {
		// poll each queue by an order
		if (model == 0) {
			queue.add(data);
			System.out.println(data[0]+"th 加入队列");
		} else if (model == 1) {
			// judge the priority of the data
			int priority = data[1];
			queues[priority - 1].add(data);
			// make sure dequeue one item
			System.out.println("count:" + count + "\tqueue size:"
					+ queue.size());
			System.out.println("id:" + queues[priority - 1].peek()[0]
					+ "\tpriority:" + priority);
			int[] choice = { 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 3, 3, 4 };
			int index = count % 15;
			if (!queues[choice[index]].isEmpty()) {
				System.out.println("not empty");

				queue.add(queues[choice[index]].poll());
			} else {
				for (int i = 0; i < queues.length; i++) {
					if (!queues[i].isEmpty()) {
						queue.add(queues[i].poll());
						System.out.println("i queue:" + i);
						break;
					}
				}
			}
			System.out.println("enqueue over, queue size:" + queue.size());
			System.out.println("id:" + queue.peek());
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
//						System.out.println("accept");
						System.out.println("set connection with generator...");
						InputStream is = socket.getInputStream();
						ObjectInputStream ois = new ObjectInputStream(is);
						Integer[] input = (Integer[]) ois.readObject();
						System.out.println("query_id:"+input[0]);
						// here may lead a problem, if current there is no
						// running task
						// currentTime = input[3];
						count++;
						enqueue(input);
					} catch (IOException | ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
		task.start();
	}

	public static void main(String[] args) throws Exception {
		@SuppressWarnings("unused")
		Processor processor = new Processor(1);
	}
}
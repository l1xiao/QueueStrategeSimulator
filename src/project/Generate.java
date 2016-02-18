package project;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class Generate {
	public Socket processorServer, timeServer;
	static int PORT1 = 8080;
	static int PORT2 = 8081;
	static Integer id = 0;
	static Integer endTime = 1000;
	static Integer currentTime = 0;
	static Integer lastTime = 0;
	Random randomno = new Random(22);

	// generate a data [id, priority, tolerance, sendtime, cost];
	public Integer[] generateData() throws Exception, IOException {

		Integer priority = (int) (1 / (1 + Math.exp(-randomno.nextGaussian())) * 4) + 1;
		Integer tolerance = (int) (1 / (1 + Math.exp(-randomno.nextGaussian()))) * 10 + 1;
		// Integer tolerance = randomno.nextInt(40) + 1;
		int cost = tolerance / 2 + 1;
		Integer[] data = new Integer[5];
		data[0] = id;
		id++;
		data[1] = priority;
		data[2] = tolerance;
		// cost
		data[4] = cost;
		lastTime = lastTime + randomno.nextInt(10);
		// send time
		data[3] = lastTime;

		return data;
	}

	public void sendData(Integer[] data) throws IOException {
		processorServer = new Socket("localhost", PORT1);
		OutputStream os = processorServer.getOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(os);
		oos.writeObject(data);
		oos.close();
		os.close();

	}

	public int getTime(int request) throws IOException, Exception {
		timeServer = new Socket("localhost", PORT2);
		OutputStream os = timeServer.getOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(os);
		Integer[] data = { 1, request };
		oos.writeObject(data);
		InputStream is = timeServer.getInputStream();
		ObjectInputStream ois = new ObjectInputStream(is);
		Integer time = (Integer) ois.readObject();
		oos.close();
		os.close();
		ois.close();
		is.close();

		return time;
	}

	public static void main(String[] args) throws Exception {
		Generate client = new Generate();
		// set up connection
		// client.connect();
		Queue<Integer[]> q = new LinkedList<Integer[]>();
		// 是否还要生成新数据
		boolean flag = true;
		while (true) {
			// check quota
			Integer[] data;
			if (currentTime < endTime && flag) {
				data = client.generateData();
				if (data[3] < endTime) {
					q.add(data);
				} else {
					flag = false;
				}
			}
			System.out.println("队列还有：" + q.size() + "当前时间：" + currentTime);
			// 发送队列不空，尝试发送请求
			if (!q.isEmpty()) {
				int serverTime = client.getTime(q.peek()[3]);
				if (serverTime < q.peek()[3]) {
					// do nothing
				} else {
					// update time
					currentTime = serverTime;
					// send to processorServer;
					client.sendData(q.poll());
				}
			}
			// 队列空了且不再生成新数据
			else if (!flag) {
				client.getTime(endTime + 1);
				break;
			}
		}
	}
}
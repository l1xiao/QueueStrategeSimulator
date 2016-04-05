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
	static Integer endTime = Setting.endTime;
	static Integer currentTime = 0;
	static Integer lastTime = 0;
	Random randomno = new Random(22);

	// generate a data [id, priority, tolerance, sendtime, cost];
	public Integer[] generateData() throws Exception, IOException {

		Integer priority = Setting.getPriority();
		Integer tolerance = Setting.getTolerance();
		// Integer tolerance = randomno.nextInt(40) + 1;
		Integer[] data = new Integer[5];
		data[0] = id;
		id++;
		data[1] = priority;
		data[2] = tolerance;
		// send time
		lastTime = lastTime + randomno.nextInt(10);
		data[3] = lastTime;
		// cost
		data[4] = Setting.getCost(tolerance);
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
			// if send queue is not empty, try to send data
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
			// queue is empty and not generate new data
			else if (!flag) {
				client.getTime(endTime + 1);
				break;
			}
		}
	}
}
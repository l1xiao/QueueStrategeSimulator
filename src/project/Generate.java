package project;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class Generate {
	public Socket socket1, socket2;
	static int PORT1 = 8080;
	static int PORT2 = 8081;
	static Integer id = 0;
	static Integer endTime = 10000;
	static Integer currentTime = 0;
	static Integer lastTime = 0;

	public Generate() {
		// connect to time server as client
		// connect to processor as client
	}

	public void connect() throws UnknownHostException, IOException {
		socket1 = new Socket("127.0.0.1", PORT1);
		socket2 = new Socket("127.0.0.1", PORT2);
	}

	// generate a data [id, priority, tolerance, generateTime, cost];
	public Integer[] generateData() {
		Random randomno = new Random();
		Integer priority = (int) (1 / (Math.exp(-randomno.nextGaussian()))) * 4 + 1;
		Integer tolerance = (int) (1 / (Math.exp(-randomno.nextGaussian()))) * 6000 + 1;
		Integer[] data = new Integer[3];
		data[0] = id;
		id++;
		data[1] = priority;
		data[2] = tolerance;
		
		lastTime = lastTime + randomno.nextInt();
		data[3] = lastTime;
		return data;
	}

	public void sendData(Integer[] data) throws IOException {
		OutputStream os = socket1.getOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(os);
		oos.writeObject(data);
		oos.close();
		os.close();
	}
	public int getTime(int request) throws IOException, Exception {
		OutputStream os = socket2.getOutputStream();
		InputStream is = socket2.getInputStream();
		ObjectInputStream ois = new ObjectInputStream(is);
		ObjectOutputStream oos = new ObjectOutputStream(os);
		Integer[] data = {0, request};
		oos.writeObject(data);
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
		client.connect();
		Queue<Integer[]> q = new LinkedList<Integer[]>();
		while (true) {
			// check quota
			Integer[] data;
			if (currentTime < endTime) {
				data = client.generateData();
				if (data[3] < endTime) {
					q.add(data);					
				}
			}
			if (!q.isEmpty()) {
				int serverTime = client.getTime(q.peek()[3]);
				if (serverTime < q.peek()[3]) {
					// do nothing
				} else {
					// update time
					currentTime = serverTime;
					// send to processor;
					client.sendData(q.poll());
				}
			}
		}
	}
}
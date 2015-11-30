package project;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

public class Processor {
	private static ServerSocket server;
	private static Socket client;
	static int PORT1 = 8080;
	static int PORT2 = 8081;
	static Integer endTime = 10000;
	static Integer currentTime = 0;
	static Queue<Integer[]> queue = new LinkedList<>();
	public Processor() throws Exception {
		server = new ServerSocket(PORT1);
		client = new Socket("127.0.0.1", PORT2);
		process();
	}
	
	public int getTime(int request) throws IOException, Exception {
		OutputStream os = client.getOutputStream();
		InputStream is = client.getInputStream();
		ObjectInputStream ois = new ObjectInputStream(is);
		ObjectOutputStream oos = new ObjectOutputStream(os);
		Integer[] data = {0, request};
		oos.writeObject(data);
		Integer time = (Integer) ois.readObject();
//		oos.close();
//		os.close();
//		ois.close();
//		is.close();
		return time;
	}
	public void process() throws IOException, Exception {
		while (currentTime < endTime) {
			if (!queue.isEmpty()) {
				int serverTime = getTime(queue.peek()[2] / 2 + currentTime);
				if (serverTime < queue.peek()[2] / 2 + currentTime) {
					// do nothing
				} else {
					currentTime = serverTime;
					queue.poll();
				}
			}
		}
	}
	public void listen(Socket socket) {
		Thread task = new Thread() {
			public void run() {
				while (true) {
					try {
						Socket socket = server.accept();
						InputStream is = socket.getInputStream();
						ObjectInputStream ois = new ObjectInputStream(is);
						Integer[] input = (Integer[]) ois.readObject();
						queue.add(input);
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
		Processor processor = new Processor();
	}
}

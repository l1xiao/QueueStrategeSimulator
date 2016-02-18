package project;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TimeServer {
	private static int timeStamp, processTime, generateTime;
	private static int endTime = 3000;
	private static int timePort = 8081;

	public TimeServer() throws IOException {
		// TODO Auto-generated constructor stub
		timeStamp = 0;
		processTime = -1;
		@SuppressWarnings("resource")
		ServerSocket server = new ServerSocket(timePort);
		while (true) {
			if (timeStamp >= endTime) {
				return;
			}
			System.out.println("server lauching, waiting for client...");
			Socket socket = server.accept();
			response(socket);

		}
	}

	private void response(Socket client) {
		System.out.println("accept a client");
		Thread task = new Thread() {
			public void run() {
				System.out.println("thread run()");
				InputStream is;
				try {
					is = client.getInputStream();
					OutputStream os = client.getOutputStream();
					ObjectInputStream ois = new ObjectInputStream(is);
					ObjectOutputStream oos = new ObjectOutputStream(os);

					// read request
					Integer[] input = (Integer[]) ois.readObject();
					System.out.println("receive data: " + input[0] + " "
							+ input[1]);
					int time = input[1];
					int cli = input[0];
					int currentTime = getTime(time, cli);
					oos.writeObject(currentTime);
					os.close();
					ois.close();
					oos.close();
					if (currentTime == endTime) {
						return;
					}
					// answer request
				} catch (IOException | ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		task.start();

	}

	private synchronized static int getTime(int time, int client) {
		if (client == 0) {
			processTime = time;
		} else if (client == 1) {
			generateTime = time;
		}
		System.out.println("generateTime:" + generateTime);
		System.out.println("processTime:" + processTime);
		// first time request from generate
		if (processTime == -1) {
			timeStamp = generateTime;
			processTime = generateTime;
		} else {
			timeStamp = Math.min(processTime, generateTime);
		}
		System.out.println("current time: " + timeStamp);
		return timeStamp;
	}

	public static void main(String[] args) throws IOException {
		@SuppressWarnings("unused")
		TimeServer server = new TimeServer();
	}
}
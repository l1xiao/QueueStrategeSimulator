package project;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TimeServer { 
	private static int timeStamp, timeStamp1, timeStamp2;
	private static int port = 8081;
	public TimeServer() throws IOException {
		// TODO Auto-generated constructor stub
		timeStamp = 0; 
		ServerSocket server = new ServerSocket(port);
		while (true) {
			System.out.println("server lauching, waiting for client...");
			Socket socket = server.accept();  
            response(socket);
		}
	}
	private void response(Socket client) {
		System.out.println("accept a client");
		Thread task = new Thread() {
			public void run() {
				InputStream is;
				try {
					is = client.getInputStream();
					OutputStream os = client.getOutputStream();
					ObjectInputStream ois = new ObjectInputStream(is);
					ObjectOutputStream oos = new ObjectOutputStream(os);
					
					// read request
					Integer[] input = (Integer[]) ois.readObject();
					int time = input[1];
					int cli = input[0];

					int currentTime = getTime(time, cli);
					oos.writeObject(currentTime);
					// answer request
					client.close();
				} catch (IOException | ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
			}
		};
		task.start();
	}

	private static int getTime(int time, int client) {
		if (client == 0) {
			timeStamp1 = timeStamp;
		} else if (client == 1) {
			timeStamp2 = timeStamp;
		}
		timeStamp = Math.min(timeStamp1, timeStamp2);
		return timeStamp;
	}
	public static void main(String[] args) throws IOException {
		TimeServer server = new TimeServer();
	}
}
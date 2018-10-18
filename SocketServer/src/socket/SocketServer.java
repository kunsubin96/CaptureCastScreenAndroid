package socket;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;

public class SocketServer {
	private static final String LOCATION = "D:\\temp\\";
	public static void main(String[] args) throws InterruptedException {
		System.out.println("Listening on port 5905, CRTL-C to stop");
		int count = 0;
		try {
			@SuppressWarnings("resource")
			ServerSocket serverSocket = new ServerSocket(5905);
			Socket socket = serverSocket.accept();
			System.out.println("got a connection");
			while (true) {
				InputStream inputStream = socket.getInputStream();

				int nRead;
				
				byte[] headerSize = new byte[6];
				DataInputStream dataISHeader = new DataInputStream(inputStream);
				dataISHeader.readFully(headerSize);
				String header=new String(headerSize);
				
				byte[] data=new byte[Integer.parseInt(header.trim())];
				DataInputStream dataIS = new DataInputStream(inputStream);
				dataIS.readFully(data);
				
				//saveFile
				writeBytesToFileNio(data,LOCATION+count+".jpg");
				count++;
				Thread.sleep(500);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private static void writeBytesToFileNio(byte[] bFile, String fileDest) {
		try {
			Path path = Paths.get(fileDest);
			Files.write(path, bFile);
			System.out.println("Write file success!");
		} catch (IOException e) {
			System.out.println("Write file fail!");
			e.printStackTrace();
		}

	}
}

package socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

public class SocketServer {
	private static final String UPLOAD_FOLDER = "D:\\temp\\";

	public static void main(String[] args) {
		System.out.println("Listening on port 5905, CRTL-C to stop");
		int i = 0;
		try {
			@SuppressWarnings("resource")
			ServerSocket serverSocket = new ServerSocket(5905);
			Socket socket = serverSocket.accept();
			System.out.println("got a connection");
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			String line;
			StringBuilder builder = new StringBuilder("");
			while ((line = reader.readLine()) != null) {
				// System.out.println(line);
				int index = line.indexOf("  ");
				if (index >= 0) {
					String temp = line.substring(0, index);
					builder.append(temp);
					byte[] decodedBytes = Base64.getDecoder().decode(builder.toString().trim());
				    writeBytesToFileNio(decodedBytes, UPLOAD_FOLDER+ i +".jpg");
					i++;
					builder.setLength(0);
					builder.append(line.substring(index));
				} else {
					builder.append(line);
				}

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

	private static String generateUUID() {
		String uuid = UUID.randomUUID().toString().replace("-", "");
		return uuid;
	}
}

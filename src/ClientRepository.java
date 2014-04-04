import java.awt.TrayIcon.MessageType;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ClientRepository extends Repository {
	private static final String remoteFileName = "Remote";
	private static final String getCheckOutString = "getCheckout";
	private boolean hasServer;
	private String serverIP;
	private int serverPort;

	public ClientRepository(String path) throws Exception {
		super(path);
		File remote = new File(path + foldername + remoteFileName);
		if (remote.exists()) {
			hasServer = true;
			BufferedReader br = new BufferedReader(new FileReader(path
					+ foldername + filesfilename));
			try {
				serverIP = br.readLine();
				serverPort = Integer.parseInt(br.readLine());
			} catch (IOException e) {
				System.err.println("Bad remote file: " + e.getMessage());
			}
		}
	}

	@Override
	public void addCommit(Commit c) {
		String toWrite = c.writeToString();
		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(path
					+ foldername + commitsFileName, true));
			output.append(toWrite);
			output.newLine();
			output.close();
		} catch (IOException e) {
			System.err.println("Cannot open commits file:" + e.getMessage());
		}
	}

	public void checkout(String ip, int port) {

		Message mes = new Message(getCheckOutString, Message.Type.Info, null);

		this.serverIP = ip;
		this.serverPort = port;
		try {
			// Send message
			Message response = sendMessageToRemote(mes);
			//write to remote file
			createRemoteFile();

			// Process message response
			if (response.getType() == Message.Type.Info) {
				for(String file : response.getContentArray()){
					
				}
			} else {
				System.err.println("Incorrect response");
			}
		} catch (IOException e) {
			System.err.println("Error: Could not checkout, " + e.getMessage());
		} catch (ClassNotFoundException e) {
			System.err.println("Error: Problem with server, please try again");
		}
	}

	private Message sendMessageToRemote(Message mes) throws IOException,
			ClassNotFoundException {
		InetSocketAddress serverAddress = new InetSocketAddress(serverIP,
				serverPort);
		Socket socket = new Socket();
		Message response;
		socket.connect(serverAddress); 

		try {
			InputStream rawInput = socket.getInputStream();
			OutputStream rawOutput = socket.getOutputStream();

			ObjectInputStream in = new ObjectInputStream(rawInput);
			ObjectOutputStream out = new ObjectOutputStream(rawOutput);

			out.writeObject(mes);

			response = (Message) in.readObject();

		} finally {
			socket.close();
		}

		return response;
	}
	
	private File requestFile(String filename) throws IOException{
		InetSocketAddress serverAddress = new InetSocketAddress(serverIP,
				serverPort);
		Socket socket = new Socket();
		Message mes = new Message(filename, Message.Type.FileRequest, "");
		socket.connect(serverAddress);
		
		File ret;
		
		try {
			InputStream rawInput = socket.getInputStream();
			OutputStream rawOutput = socket.getOutputStream();

			FileOutputStream fout = new FileOutputStream(path + foldername + lastcommitfilesdirname + filename);
			ObjectOutputStream out = new ObjectOutputStream(rawOutput);

			out.writeObject(mes);

			ret = new File(path + foldername + lastcommitfilesdirname + filename);
			

		} finally {
			socket.close();
		}
		
		return ret;
	}

	private void createRemoteFile() {
		BufferedWriter output;
		try {
			output = new BufferedWriter(new FileWriter(path + foldername
					+ commitsFileName, true));
			output.append(serverIP);
			output.newLine();
			output.append(((Integer) serverPort).toString());
			output.newLine();
			output.close();
		} catch (IOException e) {
			System.err.println("Something went wrong writing remote files");
		}
	}
}

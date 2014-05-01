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
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class ClientRepository extends Repository {
	private static final String remoteFileName = "Remote";
	public static final String getCheckOutString = "getCheckout";
	public static final String getLastCommitString = "getLastCommit";
	private boolean hasServer;
	private InetAddress serverIP;
	private int serverPort;

	public ClientRepository(String path) throws Exception {
		super(path);
		File remote = new File(path + foldername + remoteFileName);
		if (remote.exists()) {
			hasServer = true;
			BufferedReader br = new BufferedReader(new FileReader(path
					+ foldername + filesfilename));
			try {
				serverIP = InetAddress.getByName(br.readLine());
				serverPort = Integer.parseInt(br.readLine());
			} catch (IOException e) {
				System.err.println("Bad remote file: " + e.getMessage());
			}
		}
	}

	@Override
	public void addCommit(Commit c) {
		String toWrite = c.writeToString();
		// write commit to commits
		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(path
					+ foldername + commitsFileName, true));
			output.append(toWrite);
			output.newLine();
			output.close();
		} catch (IOException e) {
			System.err.println("Cannot open commits file:" + e.getMessage());
		}
		// copy latest to latest folder
		for (File f : c.getFiles()) {
			try {
				FileUtils.copyFile(f, new File(path + lastcommitfilesdirname
						+ f.getName()));
			} catch (IOException e) {
				System.err.println("Error copying file (" + f.getAbsolutePath()
						+ ") to inside dir: " + e.getMessage());
			}
		}

		// send to server
		if (hasServer) {

		}
	}

	public String status() {
		String ret = "Current Files in repository: \n";
		for (File f : files) {
			ret += f.getAbsolutePath() + "\n";
		}
		return "";
	}

	public void addFile(File f) {
		if (f.exists()) { // file should exist
			if (!files.contains(f)) { // no doubles
				files.add(f);
				BufferedWriter output;
				try {
					output = new BufferedWriter(new FileWriter(path
							+ foldername + filesfilename, true));
					output.append(f.getAbsolutePath());
					output.newLine();
					output.close();
				} catch (IOException e) {
					System.err.println("Error: Opening file file");
				}
			} else {
				System.out.println("File was alreay in the repository");
			}
		} else {
			System.out.println("File does not exist!");
		}
	}

	public void checkout(String ip, int port) {

		System.out.println("Checkout on ip: " + ip + " and port: " + port);

		Message mes = new Message(getCheckOutString, Message.Type.INFO, null);
		try {
			// Can throw a parse exception
			this.serverIP = InetAddress.getByName(ip);
			this.serverPort = port;
			this.hasServer = true;
			
			// Send messageInetAddress ip = InetAddress.getByName(args[0]);
			Message response = sendMessageToRemote(mes);
			
			System.out.println("Got response from server, start downloading files!");
			
			// write to remote file
			createRemoteFile();

			// Process message response
			if (response.getType() == Message.Type.INFO) {
				for (String file : response.getContentArray()) {
					
					System.out.println("Downloading file " + file);
					
					// loads file into repo and then copies to the working dir
					FileUtils.copyFile(requestFile(file, getSocket()),
							new File(path + file));
					
					//add file to files list
					addFile(new File(path + file));
				}
			} else {
				System.err.println("Incorrect response");
			}
		} catch (ConnectException e) {
			System.err.println("Error: Could not checkout, " + e.getMessage());
		} catch (IOException e) {
			System.err.println("Error: Could not checkout, " + e.getMessage());
		} catch (ClassNotFoundException e) {
			System.err.println("Error: Problem with server, please try again");
		}
	}

	/**
	 * 
	 * 
	 * @param f
	 * @return boolean that sais if this file is in the repo
	 */
	public boolean hasFile(File f) {
		return files.contains(f);
	}

	/**
	 * Sends a message object to the remote server
	 * 
	 * @param mes
	 *            Message object to send to remote
	 * @return Message returned by the server
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private Message sendMessageToRemote(Message mes) throws IOException,
			ClassNotFoundException, ConnectException {
		Socket socket = getSocket();

		if (socket == null) {
			
			throw new ConnectException("Could not connect to remote!");
			
		} else {

			Message response;

			try {
				
				System.out.println("Sending Message (" + mes.getContent() + ")");
				
				InputStream rawInput = socket.getInputStream();
				OutputStream rawOutput = socket.getOutputStream();

				
				ObjectOutputStream out = new ObjectOutputStream(rawOutput);
				
				out.writeObject(mes);
				out.flush();
				
				//do not open if can't collect something
				ObjectInputStream in = new ObjectInputStream(rawInput);
				response = (Message) in.readObject();

			} finally {
				socket.close();
			}
			return response;
		}
	}

	/**
	 * @return the socket for the server and port if they are set, if they are
	 *         not set we return null
	 * @throws IOException
	 */
	private Socket getSocket() throws IOException {
		if (hasServer) {
			InetSocketAddress serverAddress = new InetSocketAddress(serverIP,
					serverPort);
			Socket socket = new Socket();
			socket.connect(serverAddress);
			return socket;
		} else {
			return null;
		}
	}

	/**
	 * Creates the remote file so that we can use this remote in the future
	 */
	private void createRemoteFile() {
		BufferedWriter output;
		try {
			output = new BufferedWriter(new FileWriter(path + foldername
					+ commitsFileName, true));
			output.append(serverIP.getHostAddress());
			output.newLine();
			output.append(((Integer) serverPort).toString());
			output.newLine();
			output.close();
		} catch (IOException e) {
			System.err.println("Something went wrong writing remote files");
		}
	}
}

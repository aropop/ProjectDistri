import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class ClientRepository extends Repository {

	// Constants
	private static final String remoteFileName = "Remote";

	public static final String getCheckOutString = "getCheckout";
	public static final String getLastCommitString = "getLastCommit";
	public static final String getCommitFile = "getCommitFile";
	public static final String newCommitMessage = "newCommit";
	public static final String sendFilesString = "sendingFiles";
	public static final String listACommitsString = "listAllCommits";
	public static final String getOldFile = "getOldFile";

	public static final String fileIdSplit = "##";

	// Enums
	private enum CommitProblems {
		DIFF, // Server is commit ahead
		CON, // Could not reach the server
		SERVER_ERR, // Server responded with an error
		NONE // No error
	}

	// Data Members
	private boolean hasServer;
	private InetAddress serverIP;
	private int serverPort;

	/**
	 * Constructor
	 * 
	 * @param path
	 * @throws Exception
	 */
	public ClientRepository(String path) throws Exception {

		super(path);
		File remote = new File(this.path + foldername + remoteFileName);
		if (remote.exists()) {
			hasServer = true;
			BufferedReader br = new BufferedReader(new FileReader(this.path + foldername
					+ remoteFileName));
			try {
				serverIP = InetAddress.getByName(br.readLine());
				serverPort = Integer.parseInt(br.readLine());
			} catch (IOException e) {
				System.err.println("Bad remote file: " + e.getMessage());
			}
		}
	}

	/**
	 * Adds a commit, if we have a server it checks if we are not ahead of the
	 * latest commit
	 * 
	 * @param message
	 *            Message to add to the commit
	 * @param filesCommited
	 *            List of strings, these are the relative paths to the files
	 *            TODO: fix problem when server is 2 commits behind
	 */
	public void addCommit(String message, ArrayList<String> filesCommited) {

		Commit c = new Commit(message, new Date(), UUID.randomUUID());

		int counter = 0;
		for (String fn : filesCommited) {
			// try file in path
			File f = new File(path + fn);
			// try absolute path
			File f2 = new File(fn);

			// Check if file exists
			if (f.exists() && (hasFile(f) || hasFile(f2))) {

				c.addFile(f.getAbsolutePath().substring(path.length()));

			} else if (f2.exists() && (hasFile(f2) || hasFile(f))) {

				c.addFile(f2.getAbsolutePath().substring(path.length()));
				// Make sure no absolute paths are used
				filesCommited.set(counter, fn.substring(path.length()));

			} else {
				System.out.println("The given file (" + fn
						+ ") does not exists or is not yet added to this repository!");
				return;
			}

			counter++;

		}

		String toWrite = c.writeToString();

		// Defines whether we can go through with this commit
		CommitProblems problem = CommitProblems.NONE;
		ArrayList<String> filesWithDiff = new ArrayList<String>();

		// Check with server and update it
		if (hasServer) {

			try {

				// check if there is not interference between commits
				Message checkLatestCommit = new Message(getLastCommitString, Message.Type.INFO,
						null);
				Message fileList = sendMessageToRemote(checkLatestCommit);

				// when no file list is returned (no files in server repository)
				if (fileList.getContent().length() != 0) {

					String[] contentsArray = fileList.getContentArray();

					for (String fileIdTuple : contentsArray) {

						// List the files and check if we were on the same
						// commit

						String[] tuple = fileIdTuple.split(fileIdSplit);
						String file = tuple[0];
						String commitId = tuple[1];

						String localCommitId = files.get(new File(path + file)).toString();

						if (!localCommitId.equals(commitId)) {

							// Server is ahead of local repository
							problem = CommitProblems.DIFF;

							filesWithDiff.add(file);

						}

					}
				}

				// If we did not encounter a problem we send the new files to
				// the server
				if (problem == CommitProblems.NONE) {

					sendFilesAndCommitToServer(c);

				}

			} catch (Exception e) {
				System.out
						.println("Failed to send commit to server, will retry when connection is restored ("
								+ e.getMessage() + ")");
				e.printStackTrace();
				// sendMessageLater(newComMes);
				problem = CommitProblems.CON;
			}

		}

		if (problem == CommitProblems.NONE) {

			// Add commit to commits map
			commits.put(c.getId(), c);

			// write commit to commits
			try {

				BufferedWriter output = new BufferedWriter(new FileWriter(path + foldername
						+ commitsFileName, true));

				output.append(toWrite);
				output.newLine();

				output.close();
			} catch (IOException e) {
				System.err.println("Cannot open commits file:" + e.getMessage());
			}

			// copy latest to latest folder
			for (String f : c.getFiles()) {

				// Overwrite the latest commit locally
				files.put(new File(path + f), c.getId());

				try {

					// this will overwritten
					FileUtils.copyFile(new File(path + f), new File(path + lastcommitfilesdirname
							+ f));
				} catch (IOException e) {
					System.err.println("Error copying file (" + path + f + ") to inside dir: "
							+ e.getMessage());
				}
			}

			// write to files file
			writeFilesFile();

			System.out.println("Added commit (" + c.getId() + ")");

		} else if (problem == CommitProblems.CON) {
			System.out.println("There is a connection problem");
		}

	}

	/**
	 * Adds a server to this repository, also writes the file so it's persistent
	 * 
	 * @param ip
	 * @param port
	 */
	public void addRemote(String ip, int port) {

		try {
			this.serverIP = InetAddress.getByName(ip);
			this.serverPort = port;
			this.hasServer = true;
			// write to remote file
			createRemoteFile();
		} catch (UnknownHostException e) {
			System.err.println("Failed to add this remote (ip: " + ip + ", port: " + port + ")");
		}
	}

	/**
	 * Returns all the files, with the latest commit and whether it's adjusted
	 * Also returns if a server is saved
	 * 
	 * @return String the textual status
	 */
	public String status() {

		String ret = "Current Files in repository: \n";
		for (Map.Entry<File, UUID> f : files.entrySet()) {
			File latestFile = new File(path + lastcommitfilesdirname
					+ f.getKey().getAbsolutePath().substring(path.length()));
			try {
				ret += f.getKey().getAbsolutePath()
						+ (FileUtils.contentEquals(f.getKey(), latestFile) ? "" : "*")
						+ " "
						+ (f.getValue() == null ? "Not commited " : f.getValue() + " "
								+ commits.get(f.getValue()).getMessage() + " "
								+ commits.get(f.getValue()).getTime()) + "\n";
			} catch (IOException e) {
				System.out.println("Error reading files (" + e.getMessage() + ")");
			}

		}

		ret += hasServer ? "Linked to server at " + serverIP.getHostAddress() + " on port "
				+ serverPort + (heartBeat() ? " which is online" : " and is down")
				: "Not linked to any server";

		return ret;
	}

	/**
	 * Delays the sending of message due to a problem
	 * 
	 * @param newComMes
	 *            The message to be send later
	 */
	private void sendMessageLater(Message newComMes) {

		// TODO:
	}

	/**
	 * Ping the server, so we know if it's alive
	 * 
	 * @return true if server is live, false if it's down
	 */
	public boolean heartBeat() {

		Message m = new Message("", Message.Type.HEARTBEAT, null);
		Message r;
		try {
			r = sendMessageToRemote(m);
			return r != null;
		} catch (ConnectException e) {
			return false;
		} catch (IOException e) {
			return false;
		} catch (ClassNotFoundException e) {
			System.err.println("Server did not respond correctly");
			return false;
		}

	}
	
	public String diff(String file, String oldCommitId, String newCommitId){
		
		File file1, file2;
		String text1 = "", text2 = "", ret = "";
		
		// Get the files right
		file1 = getOldCommitedFile(file, UUID.fromString(oldCommitId));
		if(newCommitId == null)
			file2 = new File(path + file);
		else
			file2 = getOldCommitedFile(file, UUID.fromString(newCommitId));
		
		// Error checking
		if(file1 == null || file2 == null)
			return "Cannot get old file right now, retry later";
		else if (!file2.exists())
			return "Cannot find a file for commit id: " + newCommitId;
		else if(!file1.exists())
			 return "Cannot find a file for commit id: " + oldCommitId;
		
		// Read files into strings
		try {
			text1 = FileUtils.readFileToString(file1);
			text2 = FileUtils.readFileToString(file2);
		} catch (IOException e) {
			System.err.println("Could not show diff ("+ e.getMessage() + ")");
		}
		
		// Get the differences
		diff_match_patch dmp = new diff_match_patch();	
		LinkedList<diff_match_patch.Diff> diffs = dmp.diff_main(text1, text2);
		
		// Print them out
		for(diff_match_patch.Diff diff : diffs){
			if(diff.operation == diff_match_patch.Operation.INSERT)
				ret += "++   " + diff.text + "\n";
			else if(diff.operation == diff_match_patch.Operation.DELETE)
				ret += "--   "  + diff.text + "\n";
		}

		
		return ret.substring(0, ret.length() - 1);
	}

	/**
	 * Gets a previously committed file from the server
	 * 
	 * @param fn Filename of the file you want 
	 * @param commitID The commit where you want it from
	 * @return
	 */
	private File getOldCommitedFile(String fn, UUID commitID) {

		// Check if we have file
		File f = new File(path + oldCommitsFolderName + fn + commitID.toString());

		if (!f.exists()) {

			// Request from server
			try {
				f = requestFile(fn, commitID).getA();
			} catch (IOException e) {
				System.err.println("Could not get old file: " + e.getMessage());
				return null;
			}
			
		}
		return f;

	}

	/**
	 * Adds a file to the local repository
	 * 
	 * @param f
	 *            file to be added to the repository
	 */
	public void addFile(String f) {

		File f1 = new File(path + f);
		File f2 = new File(f);
		if (f1.exists()) { // file should exist
			if (!hasFile(f1)) { // no doubles
				files.put(f1, null); // no commit yet
				writeFilesFile();
			} else {
				System.out.println("File was alreay in the repository");
			}
		} else if (f2.exists()) { // file should exist
			if (!hasFile(f2)) { // no doubles
				files.put(f2, null); // no commit yet
				writeFilesFile();
			} else {
				System.out.println("File was alreay in the repository");
			}
		} else {
			System.out.println("File does not exist!");
		}
	}

	/**
	 * Checkout command gets files and commits from a server
	 * 
	 * @param ip
	 * @param port
	 */
	public void checkout(String ip, int port) {

		System.out.println("Checkout on ip: " + ip + " and port: " + port);

		Message mes = new Message(getCheckOutString, Message.Type.INFO, null);
		try {
			// Can throw a parse exception
			addRemote(ip, port);

			// Send messageInetAddress ip = InetAddress.getByName(args[0]);
			Message response = sendMessageToRemote(mes);

			System.out.println("Got response from server, start downloading files!");

			// Process message response
			if (response.getType() == Message.Type.INFO) {
				for (String file : response.getContentArray()) {

					System.out.println("Downloading file " + file);

					// loads file into repo and then copies to the working dir
					File newFile = new File(path + file);
					Pair<File, UUID> p = requestFile(file, null);
					FileUtils.copyFile(p.getA(), newFile);

					// add file to files list
					files.put(newFile, p.getB());
				}

				// Update commits
				Message m = sendMessageToRemote(new Message(getCommitFile, Message.Type.INFO, null));

				try {
					BufferedWriter output = new BufferedWriter(new FileWriter(path + foldername
							+ commitsFileName, true));
					output.write(m.getContent());
					output.newLine();
					output.close();
				} catch (IOException e) {
					System.err.println("Error: Updating commits file");
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
	 * Allow for checkouts when remote is already set
	 */
	public void checkout() {

		if (hasServer)
			checkout(serverIP.toString(), serverPort);
		else
			System.out.println("Add a remote first");
	}

	/**
	 * @param f
	 * @return boolean that sais if this file is in the repo
	 */
	public boolean hasFile(File f) {

		return files.containsKey(f);
	}

	public String listCommits() {

		Message list;
		try {
			list = sendMessageToRemote(new Message(listACommitsString, Message.Type.INFO, null));
		} catch (ConnectException e) {
			System.out.println("Error connecting");
			return "";
		} catch (IOException e) {
			System.out.println("IO error: " + e.getMessage());
			return "";
		} catch (ClassNotFoundException e) {
			System.out.println("Error connecting");
			return "";
		}

		return list.getContent();
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
	private Message sendMessageToRemote(Message mes) throws IOException, ClassNotFoundException,
			ConnectException {

		Socket socket = getSocket();

		if (socket == null) {

			throw new ConnectException("Could not connect to remote!");

		} else {

			Message response;

			try {
				// TODO:delete
				System.out.println("Sending Message (" + mes.getContent() + ")");

				InputStream rawInput = socket.getInputStream();
				OutputStream rawOutput = socket.getOutputStream();

				ObjectOutputStream out = new ObjectOutputStream(rawOutput);

				out.writeObject(mes);
				out.flush();

				// do not open if can't collect something
				ObjectInputStream in = new ObjectInputStream(rawInput);
				response = (Message) in.readObject();

			} finally {
				socket.close();
			}

			if (response != null)
				if (response.getType() == Message.Type.ERROR)
					System.out.println("Message recieved from the server is an error");

			return response;
		}
	}

	/**
	 * Sends a list of files to the server, ie after a commit
	 * 
	 * @param filesCommited
	 * @throws Exception
	 */
	private void sendFilesAndCommitToServer(Commit c) throws Exception {

		Socket socket = getSocket();

		if (socket == null) {

			throw new ConnectException("Could not connect to remote!");

		} else {

			OutputStream out = socket.getOutputStream();
			InputStream in = socket.getInputStream();
			ObjectOutputStream oOut = new ObjectOutputStream(out);

			String globSplit = "&&";

			// Make a list with the sizes
			String fileList = "";
			for (String f : c.getFiles()) {
				File actualFile = new File(path + f);
				fileList += f + "&" + actualFile.length() + globSplit;
			}

			// Create the message
			Message sendCommit = new Message(newCommitMessage + globSplit + c.writeToString()
					+ globSplit + fileList.substring(0, fileList.length() - globSplit.length()),
					Message.Type.INFO, globSplit);

			// Add the Files
			oOut.writeObject(sendCommit);
			oOut.flush();

			for (String f : c.getFiles()) {

				File file = new File(path + f);
				InputStream inF = new FileInputStream(file);
				IOUtils.copy(inF, oOut);

				oOut.flush();

			}

			// oOut.close();

			// do not open if can't collect something
			ObjectInputStream oIn = new ObjectInputStream(in);
			try {
				Message response = (Message) oIn.readObject();

				if (response.getType() != Message.Type.SUCCES)
					throw new Exception("Something went wrong on the server!");
			} catch (ClassNotFoundException e) {
				System.err.println("Could not read response (" + e.getMessage() + ")");
			}

			// socket.close();

		}
	}

	/**
	 * @return the socket for the server and port if they are set, if they are
	 *         not set we return null
	 * @throws IOException
	 */
	private Socket getSocket() throws IOException {

		if (hasServer) {
			InetSocketAddress serverAddress = new InetSocketAddress(serverIP, serverPort);
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

		// Create remote file if it doesn't exists
		File remoteFile = new File(path + foldername + remoteFileName);
		if (!remoteFile.exists())
			try {
				remoteFile.createNewFile();
			} catch (IOException e1) {
				System.out.println("Error creating remote file!");
			}

		// Write information to it
		BufferedWriter output;
		try {
			output = new BufferedWriter(new FileWriter(path + foldername + remoteFileName, true));
			output.append(serverIP.getHostAddress());
			output.newLine();
			output.append(((Integer) serverPort).toString());
			output.newLine();
			output.close();
		} catch (IOException e) {
			System.err.println("Something went wrong writing remote files");
		}
	}

	/**
	 * Requests a file from the server and saves it in the repository folder
	 * 
	 * @param filename
	 *            file to request from the server
	 * @param id
	 *            if this is not null we will ask for an older file
	 * @return File we have stored in the repository folder
	 * @throws IOException
	 */
	private Pair<File, UUID> requestFile(String filename, UUID id) throws IOException {

		Socket socket = getSocket();

		Message mes;
		if (id == null)
			mes = new Message(filename, Message.Type.FILEREQUEST, "");
		else
			mes = new Message(getOldFile + "&&" + filename + "&&" + id.toString(),
					Message.Type.FILEREQUEST, "&&");

		Pair<File, UUID> ret = new Pair<File, UUID>(null, null);

		try {
			InputStream rawInput = socket.getInputStream();
			OutputStream rawOutput = socket.getOutputStream();

			// Need to write to a different file
			String pathToDownloadedFile;
			if (id == null)
				pathToDownloadedFile = path + lastcommitfilesdirname + filename;
			else
				pathToDownloadedFile = path + oldCommitsFolderName + filename + id.toString();
			FileOutputStream fout = new FileOutputStream(pathToDownloadedFile);

			ObjectOutputStream out = new ObjectOutputStream(rawOutput);

			out.writeObject(mes);
			out.flush();

			ObjectInputStream in = new ObjectInputStream(rawInput);
			Message response = (Message) in.readObject();

			IOUtils.copy(rawInput, fout);

			fout.close();

			
			ret = new Pair<File, UUID>(new File(pathToDownloadedFile),
					UUID.fromString(response.getContent()));
		} catch (ClassNotFoundException e) {
			System.err.println("Error while reading response");
		} finally {
			socket.close();
		}

		return ret;
	}
}

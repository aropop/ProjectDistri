import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class ServerRepository extends Repository implements Runnable {

	private ServerSocket serverSocket;
	private int port;

	/**
	 * @param user
	 *            Identifies which user wants to use this repository, so we can
	 *            track who changed stuff
	 * @throws Exception
	 */
	public ServerRepository(String user, int port) throws Exception {

		super(user);

		// Manage Connection
		this.port = port;
		InetSocketAddress serverAddress = new InetSocketAddress(this.port);
		this.serverSocket = new ServerSocket();
		serverSocket.bind(serverAddress);

	}

	@Override
	public void run() {

		System.out.println("Succesfully Started Server!\nWaiting for connections on port " + port
				+ "...");
		while (true) {
			// TODO: mutlithreading here
			Socket clientSocket;
			try {
				clientSocket = serverSocket.accept();

				System.out.println("Got client! (port:" + port + ")");

				// we will only recieve Message objects
				ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
				OutputStream out = clientSocket.getOutputStream();

				Message inMes = (Message) in.readObject();

				System.out.println("Got Message (" + inMes.getContent() + ")");

				messageDispatch(inMes, in, out);
			} catch (IOException e) {
				System.err.println("Error: Read error " + e.getMessage());
			} catch (ClassNotFoundException e) {
				System.err.println("Error: Couldn't read class" + e.getMessage());
			}

		}
	}

	/**
	 * @param inMes
	 *            Message we got from the client
	 * @param out
	 *            OutputStream to the client, since different messages have
	 *            Different ways of getting a response
	 */
	private void messageDispatch(Message inMes, ObjectInputStream in, OutputStream out) {

		if (inMes.getType() == Message.Type.ERROR) {

			// just print error, we don't really bother
			System.out.println(inMes.getContent());

		} else if (inMes.getType() == Message.Type.FILEREQUEST) {
			// file request

			if (inMes.getContent().contains(ClientRepository.getOldFile)
					&& !inMes.getContent().contains(path + foldername)) {

				// Get old commited file

				String[] arr = inMes.getContentArray();
				String file = arr[1], commit = arr[2];

				sendFile(new File(path + oldCommitsFolderName + file + commit), out, commit);

			} else if (inMes.getContent().contains(path + foldername))
				// Client cannot get every file here for security reasons
				try {
					out.write(0);
				} catch (IOException e) {
					System.out.println("Write error!" + e.getMessage());
				}
			else
				sendFile(new File(path + inMes.getContent()), out, null);

		} else if (inMes.getType() == Message.Type.HEARTBEAT) {
			// file request

			try {
				(new ObjectOutputStream(out))
						.writeObject(new Message("", Message.Type.SUCCES, null));
			} catch (IOException e) {
				sendError("ERR HEARTBEAT", out);
			}

		} else if ((inMes.getType() == Message.Type.INFO)
				&& inMes.getContent().equals(ClientRepository.listACommitsString)) {
			// List commits
			System.out.println("Requesting a list of commits");
			buildListOfCommits(out);

		} else if ((inMes.getType() == Message.Type.INFO)
				&& inMes.getContent().equals(ClientRepository.getCheckOutString)) {
			// Checkout
			System.out.println("Requesting Checkout");
			try {
				sendCheckout(new ObjectOutputStream(out));
			} catch (IOException e) {
				System.out.println("Error: creating object output stream" + e.getMessage());
			}
		} else if ((inMes.getType() == Message.Type.INFO)
				&& inMes.getContent().equals(ClientRepository.getCommitFile)) {

			// Request commits file while checking out
			Message rMes;
			try {
				rMes = new Message(FileUtils.readFileToString(new File(path + foldername
						+ commitsFileName)), Message.Type.INFO, null);
				(new ObjectOutputStream(out)).writeObject(rMes);
			} catch (IOException e) {
				System.out.println("Error sending commit info (" + e.getMessage() + ")");
			}

		} else if ((inMes.getType() == Message.Type.INFO)
				&& inMes.getContentArray()[0].equals(ClientRepository.newCommitMessage)) {

			// Add the commit
			Commit newCom = new Commit(null, null, null);
			newCom.readFromString(new ArrayList<String>(Arrays.asList(inMes.getContentArray()[1]
					.split("\n"))));
			addCommit(newCom, in, out, Arrays.copyOfRange(	inMes.getContentArray(), 2,
															inMes.getContentArray().length));

		} else if ((inMes.getType() == Message.Type.INFO)
				&& inMes.getContentArray()[0].equals(ClientRepository.getLastCommitString)) {

			// Send the last files including their latest id
			sendLatestCommitString(out);

		} else {
			System.err.println("Unknown Message, sending error");
			sendError("UNKNOWN MESSAGE", out);
		}

		try {
			out.close();
		} catch (IOException e) {
			System.err.println("Could not send, trying to send error message");
			sendError("ERROR FLUSHING", out);
		}
	}

	/**
	 * Writes a message with a list of all commits
	 * 
	 * @param out
	 */
	private void buildListOfCommits(OutputStream out) {

		String ret = "                 ID                  |              Date             |         Message \n";

		// Make sure we sort the set so we give an ordered overview
		SortedSet<Map.Entry<UUID, Commit>> sortedEntries = new TreeSet<Map.Entry<UUID, Commit>>(
				new Comparator<Map.Entry<UUID, Commit>>() {

					@Override
					public int compare(Map.Entry<UUID, Commit> c1, Map.Entry<UUID, Commit> c2) {

						return c1.getValue().getTime().compareTo(c2.getValue().getTime());
					}
				});
		sortedEntries.addAll(commits.entrySet());

		for (Map.Entry<UUID, Commit> entr : sortedEntries) {

			ret += entr.getKey() + " | " + entr.getValue().getTime().toString() + " | "
					+ entr.getValue().getMessage() + "\n";

			ArrayList<String> fs = entr.getValue().getFiles();

			ret += "Files: \n";

			for (String fn : fs)
				ret += "         ->     " + fn + "\n";

		}

		try {
			(new ObjectOutputStream(out)).writeObject(new Message(ret, Message.Type.INFO, null));
		} catch (IOException e) {
			System.out.println("IO err : (" + e.getMessage() + ")");
			sendError("IO ERR", out);
		}

	}

	private void sendLatestCommitString(OutputStream out) {

		String mesCont = "";
		String sep = "&&";

		for (Map.Entry<File, UUID> entr : files.entrySet()) {

			mesCont += entr.getKey().getAbsolutePath().substring(path.length())
					+ ClientRepository.fileIdSplit + entr.getValue() + sep;

		}

		Message mes;
		// no files can still be on the server
		if (files.size() != 0)
			mes = new Message(mesCont.substring(0, mesCont.length() - sep.length()),
					Message.Type.INFO, sep);
		else
			mes = new Message("", Message.Type.INFO, null);

		ObjectOutputStream oo;
		try {
			oo = new ObjectOutputStream(out);
			oo.writeObject(mes);
			oo.flush();
		} catch (IOException e) {
			System.err.println("Error while sending latest commit string");
			sendError("ERROR IO", out);
		}
	}

	/**
	 * Receives Various files from a client
	 * 
	 * @param in
	 * @param out
	 * @param inMes
	 *            Message contains filenames and sizes
	 */
	private void recieveFiles(InputStream in, OutputStream out, String[] fileNamesAndSizes) {

		// Make and fill each files
		for (String fiAndSize : fileNamesAndSizes) {

			if (!fiAndSize.equals("") && fiAndSize.contains("&")) {

				String fi = fiAndSize.split("&")[0];
				long size = Integer.parseInt(fiAndSize.split("&")[1]);

				File existingFile = new File(path + lastcommitfilesdirname + fi);

				try {
					// Copy old file
					if (existingFile.exists())
						existingFile.renameTo(new File(path + oldCommitsFolderName + fi
								+ this.files.get(new File(path + fi))));
					else { // Support for files in folders, by creating these folders
						existingFile.getParentFile().mkdirs();
						(new File(path + oldCommitsFolderName + fi)).getParentFile().mkdirs();
					}

					FileOutputStream fout = new FileOutputStream(path + lastcommitfilesdirname + fi);

					byte[] tst = new byte[(int) size];
					in.read(tst);

					fout.write(tst);

					// Copy file to working dir
					FileUtils.copyFile(new File(path + lastcommitfilesdirname + fi), new File(path
							+ fi));

				} catch (FileNotFoundException e) {
					System.err.println("Error surrounding file system (" + e.getMessage() + ")");
					sendError("FILE SYSTEM ERR", out);
				} catch (IOException e) {
					System.out.println("IO exception: " + e.getMessage());
					sendError("IO ERR", out);
				}
			}
		}
	}

	private void sendError(String mes, OutputStream out) {

		Message m = new Message(mes, Message.Type.ERROR, null);
		try {
			ObjectOutputStream oo = new ObjectOutputStream(out);
			oo.writeObject(m);
			oo.flush();
			System.out.println("Succesfully send error message!");
		} catch (IOException e) {
			System.err.println("Could not send error, client will hold waiting :(");
		}

	}

	/**
	 * Sends the file names in a message to the client
	 * 
	 * @param oOut
	 *            object output stream to send the message over
	 */
	private void sendCheckout(ObjectOutputStream oOut) {

		String filesStr = "";
		for (Map.Entry<File, UUID> f : files.entrySet()) {
			filesStr += f.getKey().getAbsolutePath()
					.substring(f.getKey().getAbsolutePath().lastIndexOf(path) + path.length())
					+ "&";
		}
		try {
			oOut.writeObject(new Message(filesStr.substring(0, filesStr.length() - 1),
					Message.Type.INFO, "&"));
			oOut.flush();
			System.out.println("Send checkout Message, waiting for file requests ('"
					+ filesStr.substring(0, filesStr.length() - 1) + "')");
		} catch (IOException e) {
			System.err.println("Error: Could not write object " + e.getMessage());
			sendError("ERROR FLUSHING", (OutputStream) oOut);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see Repository#addCommit(Commit)
	 */
	public synchronized void addCommit(Commit c, InputStream in, OutputStream out,
			String[] filesAndSizes) {

		// Update files
		for (String f : c.getFiles()) {

			// Will overwrite existing files
			files.put(new File(path + f), c.getId());
		}

		// Update Commits
		commits.put(c.getId(), c);

		// Rewrite files file
		writeFilesFile();

		// Write commit in file
		String toWrite = c.writeToString();
		BufferedWriter output;
		try {
			output = new BufferedWriter(new FileWriter(path + foldername + commitsFileName, true));
			output.append(toWrite);
			output.newLine();
			output.close();
		} catch (IOException e) {
			System.out.println("Error writing commit to file" + e.getMessage());
		}

		System.out.println("Updated files");

		// Recieve files after a commit
		recieveFiles(in, out, filesAndSizes);

		System.out.println("Recieved Files, sending response");

		Message response = new Message("Succes", Message.Type.SUCCES, null);

		try {
			ObjectOutputStream oout = new ObjectOutputStream(out);
			oout.writeObject(response);
			oout.flush();

		} catch (IOException e) {
			sendError("RESPONSE ERROR", out);
			System.out.println("Could not answer IO: " + e.getMessage());
		}

	}

	/**
	 * Sends the file over the output stream, also writes the id in a message
	 * 
	 * @param f
	 *            File to send
	 * @param out
	 *            Outputstream to send over
	 * @param id
	 *            Id to give back, could not be the most recent file so it's not always in our map
	 */
	private void sendFile(File f, OutputStream out, String id) {

		InputStream in;
		try {
			ObjectOutputStream oOut = new ObjectOutputStream(out);

			// if id is given we don't look it up
			if (id == null)
				oOut.writeObject(new Message(files.get(f).toString(), Message.Type.SUCCES, null));
			else
				oOut.writeObject(new Message(id, Message.Type.SUCCES, null));

			in = new FileInputStream(f);
			IOUtils.copy(in, out);
		} catch (FileNotFoundException e) {
			System.out.println("Error sending file " + e.getMessage());
		} catch (IOException e) {
			System.out.println("Error copying streams " + e.getMessage());
		}
	}

}

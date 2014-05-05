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
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;

public class ServerRepository extends Repository implements Runnable {
	
	private static final String oldCommitsFolderName = "oldCommits/";

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
		
		// Server repository specific stuff
		File oldDir = new File(path + foldername + oldCommitsFolderName);
		if(!oldDir.exists())
			oldDir.mkdir();
		
		// Manage Connection
		this.port = port;
		InetSocketAddress serverAddress = new InetSocketAddress(this.port);
		this.serverSocket = new ServerSocket();
		serverSocket.bind(serverAddress);

	}

	@Override
	public void run() {
		System.out
				.println("Succesfully Started Server!\nWaiting for connections on port "
						+ port + "...");
		while (true) {
			// TODO: mutlithreading here
			Socket clientSocket;
			try {
				clientSocket = serverSocket.accept();

				System.out.println("Got client! (port:" + port + ")");

				// we will only recieve Message objects
				ObjectInputStream in = new ObjectInputStream(
						clientSocket.getInputStream());
				OutputStream out = clientSocket.getOutputStream();

				Message inMes = (Message) in.readObject();

				System.out.println("Got Message (" + inMes.getContent() + ")");
				
				messageDispatch(inMes, in, out);
			} catch (IOException e) {
				System.err.println("Error: Read error " + e.getMessage());
			} catch (ClassNotFoundException e) {
				System.err.println("Error: Couldn't read class"
						+ e.getMessage());
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

			sendFile(new File(path + inMes.getContent()), out);

		} else if ((inMes.getType() == Message.Type.INFO)
				&& inMes.getContent()
						.equals(ClientRepository.getCheckOutString)) {
			// Checkout
			System.out.println("Requesting Checkout");
			try {
				sendCheckout(new ObjectOutputStream(out));
			} catch (IOException e) {
				System.out.println("Error: creating object output stream"
						+ e.getMessage());
			}
		} else if ((inMes.getType() == Message.Type.INFO)
				&& inMes.getContent().equals(ClientRepository.getCommitFile)) {

			// Request commits file while checking out

			sendFile(new File(path + foldername + commitsFileName), out);

		} else if ((inMes.getType() == Message.Type.INFO)
				&& inMes.getContentArray()[0]
						.equals(ClientRepository.sendFilesString)) {

			// Recieve files after a commit

			recieveFiles(in, out, inMes);

		}else if ((inMes.getType() == Message.Type.INFO)
				&& inMes.getContentArray()[0]
						.equals(ClientRepository.getLastCommitString)) {

			// Send the last files including their latest id
			sendLatestCommitString(out);
			

		} else if ((inMes.getType() == Message.Type.INFO)
				&& inMes.getContentArray()[0]
						.equals(ClientRepository.newCommitMessage)) {

			// New commit send from client

			Commit newC = new Commit(null, null, null);
			newC.readFromString(new ArrayList<String>(Arrays.asList(inMes
					.getContentArray()[1].split("\n"))));
			addCommit(newC, in, out);

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

	private void sendLatestCommitString(OutputStream out) {
		
		String mesCont = "";
		String sep = "&&";
		
		for(Map.Entry<File, UUID> entr : files.entrySet()){
			
			mesCont += entr.getKey() + ClientRepository.fileIdSplit + entr.getValue() + sep;
			
		}
		
		Message mes;
		// no files can still be on the server
		if(files.size() != 0)
			mes = new Message (mesCont.substring(0, mesCont.length() - sep.length()), Message.Type.INFO, sep);
		else
			mes = new Message ("", Message.Type.INFO, null);
		
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
	 * Recieves Various files from a client
	 * 
	 * @param in 
	 * @param out
	 * @param inMes Message contains filenames and sizes
	 */
	private void recieveFiles(ObjectInputStream in, OutputStream out, Message inMes) {

		// Skip the 'identifier' which the dispatcher uses
		String[] files = Arrays.copyOfRange(inMes.getContentArray(), 1, inMes.getContentArray().length);
		
		// Make and fill each files
		for (String fiAndSize : files) {
			
			String fi = fiAndSize.split("&")[0];
			long size = Integer.parseInt(fiAndSize.split("&")[1]);
			
			File existingFile = new File(path + lastcommitfilesdirname + fi);
			

			try {
				// Copy old file
				if(existingFile.exists())
					existingFile.renameTo(new File(path + foldername + oldCommitsFolderName + fi + this.files.get(existingFile)));
				
				FileOutputStream fout = new FileOutputStream(path
						+ lastcommitfilesdirname + fi);
				
				
				byte[] tst = new byte[(int)size];
				in.read(tst);
				
				fout.write(tst);
				
				
				
			} catch (FileNotFoundException e) {
				System.err.println("Error surrounding file system");
				sendError("FILE SYSTEM ERR", out);
			} catch (IOException e) {
				System.out.println("IO exception: " + e.getMessage());
				sendError("IO ERR", out);
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
			System.err
					.println("Could not send error, client will hold waiting :(");
		}

	}

	/**
	 * Sends the file names in a message to the client
	 * 
	 * @param oOut
	 *            object ouput stream to send the message over
	 */
	private void sendCheckout(ObjectOutputStream oOut) {
		String filesStr = "";
		for (Map.Entry<File, UUID> f : files.entrySet()) {
			filesStr += f
					.getKey()
					.getAbsolutePath()
					.substring(
							f.getKey().getAbsolutePath().lastIndexOf(path)
									+ path.length())
					+ "&";
		}
		try {
			oOut.writeObject(new Message(filesStr.substring(0,
					filesStr.length() - 1), Message.Type.INFO, "&"));
			oOut.flush();
			System.out
					.println("Send checkout Message, waiting for file requests ('"
							+ filesStr.substring(0, filesStr.length() - 1)
							+ "')");
		} catch (IOException e) {
			System.err.println("Error: Could not write object "
					+ e.getMessage());
			sendError("ERROR FLUSHING", (OutputStream) oOut);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see Repository#addCommit(Commit)
	 */
	public synchronized void addCommit(Commit c, InputStream in,
			OutputStream out) {

		// Write commit in file
		String toWrite = c.writeToString();
		BufferedWriter output;
		try {
			output = new BufferedWriter(new FileWriter(path + foldername
					+ commitsFileName, true));
			output.append(toWrite);
			output.newLine();
			output.close();
		} catch (IOException e) {
			System.out.println("Error writing commit to file" + e.getMessage());
		}

		// Update files
		for (String f : c.getFiles()) {

		}

		Message response = new Message("Succes", Message.Type.SUCCES, null);

	}

	private void sendFile(File f, OutputStream out) {
		InputStream in;
		try {
			in = new FileInputStream(f);
			IOUtils.copy(in, out);
		} catch (FileNotFoundException e) {
			System.out.println("Error sending file " + e.getMessage());
		} catch (IOException e) {
			System.out.println("Error copying streams " + e.getMessage());
		}
	}

}

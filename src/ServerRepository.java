import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.io.IOUtils;



public class ServerRepository extends Repository {
	
	private ServerSocket serverSocket;
	private int port;
	
	/**
	 * @param user Identifies which user wants to use this repository, so we can track who changed stuff
	 * @throws Exception
	 */
	public ServerRepository(String user, int port) throws Exception{
		super(user);
		InetSocketAddress serverAddress = new InetSocketAddress(port);
		this.port = port;
		this.serverSocket = new ServerSocket();
		serverSocket.bind(serverAddress);
		clientLoop();
	}
	
	
	private void clientLoop(){
		while (true){
			//TODO: mutlithreading here
			Socket clientSocket;
			try {
				clientSocket = serverSocket.accept();
				
				//we will only recieve Message objects
				ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
				OutputStream out = clientSocket.getOutputStream();
				
				Message inMes = (Message) in.readObject();
				
				messageDispatch(inMes, out);
			} catch (IOException e) {
				System.err.println("Error: Read error " + e.getMessage());
			} catch (ClassNotFoundException e) {
				System.err.println("Error: Couldn't read class" + e.getMessage());
			} 			
		
			
			
		}
	}
	
	

	/**
	 * @param inMes Message we got from the client
	 * @param out OutputStream to the client, since different messages have
	 * 			   Different ways of getting a response
	 */
	private void messageDispatch(Message inMes, OutputStream out) {
		if(inMes.getType() == Message.Type.Error){
			//just print error, we don't really bother
			System.out.println(inMes.getContent());
			
		}else if (inMes.getType() == Message.Type.FileRequest){
			sendFile(new File(inMes.getContent()), out);
		}else if ((inMes.getType() == Message.Type.Info) && inMes.getContent().equals(ClientRepository.getCheckOutString)){
			try {
				sendCheckout(new ObjectOutputStream(out));
			} catch (IOException e) {
				System.out.println("Error: creating object output stream" + e.getMessage());
			}
		}
	}


	/**
	 * Sends the file names in a message to the client
	 * 
	 * @param oOut object ouput stream to send the message over
	 */
	private void sendCheckout(ObjectOutputStream oOut) {
		String filesStr  = "";
		for(File f : files){
			filesStr += f.getName() + "&";
		}
		try {
			oOut.writeObject(new Message(filesStr.substring(0, filesStr.length() - 1), Message.Type.Info, "&"));
		} catch (IOException e) {
			System.err.println("Error: Could not write object " + e.getMessage());
		}
	}


	/* (non-Javadoc)
	 * @see Repository#addCommit(Commit)
	 */
	@Override
	public synchronized void addCommit(Commit c) {
		String toWrite  = c.writeToString();
		BufferedWriter output;
		try {
			output = new BufferedWriter(new FileWriter(path
					+ foldername + commitsFileName, true));
			output.append(toWrite);
			output.newLine();
			output.close();
		} catch (IOException e) {
			System.out.println("Error writing commit to file" + e.getMessage());
		}
	}
	
	
	
	private void sendFile(File f, OutputStream out){
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

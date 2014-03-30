import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


public class ClientRepository extends Repository {
	private static final String remoteFileName = "Remote";
	private boolean hasServer;
	private String serverIP;
	private int serverPort;
	
	public ClientRepository(String path) throws Exception{
		super(path);
		File remote = new File(path + foldername + remoteFileName);
		if(remote.exists()){
			hasServer = true;
			BufferedReader br = new BufferedReader(new FileReader(path
					+ foldername + filesfilename));
			try{
				serverIP = br.readLine();
				serverPort = Integer.parseInt(br.readLine());
			}catch (IOException e ){
				System.err.println("Bad remote file: " + e.getMessage());
			}
		}
	}

	@Override
	public void addCommit(Commit c) {
		String toWrite = c.writeToString();
		try{
			BufferedWriter output = new BufferedWriter(new FileWriter(path + foldername + commitsFileName, true));
			output.append(toWrite);
			output.close();
		}catch(IOException e){
			System.err.println("Cannot open commits file:" + e.getMessage());
		}
	}

	
	public void checkout(String ip, int port){
		
	}
}

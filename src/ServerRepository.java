import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;


public class ServerRepository extends Repository {
	
	public ServerRepository(String user) throws Exception{
		super(user);
	}

	/* (non-Javadoc)
	 * @see Repository#addCommit(Commit)
	 */
	@Override
	public void addCommit(Commit c) {
		//hier moeten checks voor integriteit gebeuren
	}
	
	
	private void sendFile(Socket socket){
		OutputStream out = socket.getOutputStream();
		InputStream in = new FileInputStream(myFile);
		IOUtils.copy(in, out);
		socket.close();
	}

}

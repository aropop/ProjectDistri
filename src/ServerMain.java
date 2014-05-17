import java.util.ArrayList;


public class ServerMain {
	
	private static final int startPort = 12345;

	/**
	 * @param args should be all repositories to start with
	 */
	public static void main(String[] args) {
		ArrayList<ServerRepository> srs = new ArrayList<ServerRepository>();
		int i = startPort;
		for (String path : args){
			try {
				ServerRepository tmp = new ServerRepository(path, i);
				srs.add(tmp);
				//make them in a thread so we can run all the server repo's at once
			    Thread t = new Thread(tmp);
			    t.start();
				i++;
			} catch (Exception e) {
				System.err.println("Could not create or open repository on "+ path + " : " + e.getMessage());
			}
		}
		
		

	}

}

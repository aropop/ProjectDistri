import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.Socket;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;

/**
 * @author Arno De Witte
 * 
 *         The repository is stored in a hidden folder within the directory
 * 
 */
public abstract class Repository {
	protected static final String foldername = ".vc/"; // vc = version control
	protected static final String filesfilename = "files";
	protected static final String lastcommitfilesdirname =  foldername + "latest/";
	protected static final String commitsFileName = "commits";
	protected static final String[] filesToCreate = {filesfilename, commitsFileName};
	protected static final String[] foldersToCreate = {foldername, lastcommitfilesdirname};
	protected String path;
	protected ArrayList<File> files;
	protected ArrayList<Commit> commits;

	/**
	 * @param path
	 *            path to where you want to open the repository
	 */
	public Repository(String path) throws Exception {
		//support directory names with no / ending
		if(!path.substring(path.length() - 1, path.length()).equals("/"))
			path = path + "/";
		
		// check path exists
		File dir = new File(path);
		if (dir.exists() && dir.isDirectory()) {
			// check if local directory exists
			File localRepDir = new File(path + foldername);
			// initialisation
			this.path = path;
			this.files = new ArrayList<File>();
			this.commits = new ArrayList<Commit>();
			if (localRepDir.exists() && localRepDir.isDirectory())
				readFromDir(path);
			else
				createFolder();
		} else {
			throw new Exception(
					"Cannot create or open repository in a directory that does not exists!");
		}
	}

	/**
	 * Reads data from an existing path
	 * 
	 * @param path
	 *            path where to read from
	 * 
	 */
	private void readFromDir(String path) {
		System.out.println("Directory already exists, reading...");
		try {
			//read files
			BufferedReader br = new BufferedReader(new FileReader(path
					+ foldername + filesfilename));
			String file = br.readLine();
			while (file != null) {
				File f = new File(file);
				if(!f.exists()){
					System.err.println("File " + file + " does not exist!");
				}
				files.add(f);
				
				file = br.readLine();
			}
			
			br.close();
			
			//read commits
			br = new BufferedReader(new FileReader(path
					+ foldername + commitsFileName));
			String commitString = br.readLine();
			while (commitString != null) {
				ArrayList<String> arr = new ArrayList<String>();
				while(commitString.substring(0,1).equals(":")){
					arr.add(commitString.substring(1));
					commitString = br.readLine();
				}
				Commit c = new Commit(null, null, null);
				c.readFromString(arr);
				commits.add(c);
				commitString = br.readLine(); //should be the END
			}
		} catch (FileNotFoundException e) {
			System.err.println("Cannot open repository, files missing");
		} catch (IOException e) {
			System.out.println("File missing");
		} 
		System.out.println("Reading succesfull!");
	}

	private void createFolder() {
		System.out.println("Folder did not exits, creating...");
		try {
			for (String fol : foldersToCreate){
				(new File(path + fol)).mkdir();
			}
			for (String fil : filesToCreate){
				(new File(path + foldername + fil)).createNewFile();				
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
		System.out.println("Creating succesfull!");
	}
	
	/**
	 * 
	 * Handles the adding of a commit
	 * 
	 * @param c
	 */
	public abstract void addCommit(Commit c);
	
	
	
	/**
	 * Requests a file from the server and saves it in the repository folder
	 * 
	 * @param filename file to request from the server
	 * @return File we have stored in the repository folder 
	 * @throws IOException
	 */
	protected File requestFile(String filename, Socket socket) throws IOException{
		
		Message mes = new Message(filename, Message.Type.FILEREQUEST, "");

		File ret;
		
		try {
			InputStream rawInput = socket.getInputStream();
			OutputStream rawOutput = socket.getOutputStream();

			FileOutputStream fout = new FileOutputStream(path + lastcommitfilesdirname + filename);
			ObjectOutputStream out = new ObjectOutputStream(rawOutput);

			out.writeObject(mes);

			
			IOUtils.copy(rawInput, fout);
			
			fout.close();

			ret = new File(path + lastcommitfilesdirname + filename);
		} finally {
			socket.close();
		}
		
		return ret;
	}
	
	

}

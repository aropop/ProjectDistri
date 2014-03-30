import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

/**
 * @author Arno De Witte
 * 
 *         The repository is stored in a hidden folder within the directory
 * 
 */
public abstract class Repository {
	protected static final String foldername = ".vc/"; // vc = version control
	protected static final String filesfilename = "files";
	protected static final String lastcommitfilesdirname = "latest/";
	protected static final String commitsFileName = "commits";
	protected String path;
	protected ArrayList<File> files;

	/**TODO Auto-generated method stub
	 * @param path
	 *            path to where you want to open the repository
	 */
	public Repository(String path) throws Exception {
		// check path exists
		File dir = new File(path);
		if (dir.exists() && dir.isDirectory()) {
			// check if local directory exists
			File localRepDir = new File(path + foldername);
			// initialisation
			this.path = path;
			this.files = new ArrayList<File>();
			if (localRepDir.exists() && localRepDir.isDirectory())
				readFromDir(path + foldername);
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
		try {
			BufferedReader br = new BufferedReader(new FileReader(path
					+ foldername + filesfilename));
			String file = br.readLine();
			while (file != null) {
				files.add(new File(path + foldername + lastcommitfilesdirname
						+ file));
				file = br.readLine();
			}
		} catch (FileNotFoundException e) {
			System.err.println("Cannot open repository, files missing");
		} catch (IOException e) {
			System.out.println("File missing");
		}
	}

	private void createFolder() {
		try {
			(new File(path + foldername)).mkdir();
			(new File(path + foldername + lastcommitfilesdirname)).mkdir();
			(new File(path + foldername + filesfilename)).createNewFile();
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}
	
	/**
	 * 
	 * Handles the adding of a commit
	 * 
	 * @param c
	 */
	public abstract void addCommit(Commit c);
	
	

}

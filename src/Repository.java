import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Arno De Witte
 * 
 *         The repository is stored in a hidden folder within the directory
 * 
 */
public abstract class Repository {
	protected static final String foldername = ".vc/"; // vc = version control
	protected static final String filesfilename = "files";
	protected static final String lastcommitfilesdirname = foldername
			+ "latest/";
	protected static final String commitsFileName = "commits";
	protected static final String oldCommitsFolderName = foldername + "oldCommits/";
	protected static final String[] filesToCreate = { filesfilename,
			commitsFileName };
	protected static final String[] foldersToCreate = { foldername, oldCommitsFolderName,
			lastcommitfilesdirname };
	protected static final String noCommitString = "NOCOMMIT";
	protected static final String filesFileSplitter = "&";
	
	protected String path;
	protected Map<File, UUID> files;
	protected Map<UUID, Commit> commits;

	/**
	 * @param path
	 *            path to where you want to open the repository
	 * @throws Exception
	 */
	public Repository(String path) throws Exception {
		this.path = path;
		// support directory names with no / ending
		if (!path.substring(path.length() - 1, path.length()).equals("/"))
			this.path = path + "/";

		// check path exists
		File dir = new File(this.path);
		
		if (dir.exists() && dir.isDirectory()) {
			
			// check if local directory exists
			File localRepDir = new File(this.path + foldername);
			
			// initialisation
			this.files = new HashMap<File, UUID>();
			this.commits = new HashMap<UUID, Commit>();
			if (localRepDir.exists() && localRepDir.isDirectory())
				readFromDir(this.path);
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
			// Read files
			BufferedReader br = new BufferedReader(new FileReader(new File(path
					+ foldername + filesfilename)));
			String fileAndId = br.readLine();
			while (fileAndId != null) {

				String[] spl = fileAndId.split(filesFileSplitter);
				String file = spl[0];
				String id = spl[1];

				UUID id_t = null;

				if (!id.equals(noCommitString)) // should not happen on a server
					id_t = UUID.fromString(id);

				File f = new File(file);
				if (!f.exists()) {
					System.err.println("File " + file + " does not exist!");
				}
				files.put(f, id_t);

				fileAndId = br.readLine();
			}

			br.close();

			// read commits
			br = new BufferedReader(new FileReader(path + foldername
					+ commitsFileName));

			String commitString = br.readLine();

			while (commitString != null) {

				ArrayList<String> arr = new ArrayList<String>();

				while (commitString.substring(0, 1).equals(":")) {

					arr.add(commitString);
					commitString = br.readLine();

				}
				arr.add(commitString); 
				
				Commit c = new Commit(null, null, null);
				c.readFromString(arr);
				commits.put(c.getId(), c);
				
				commitString = br.readLine();
			}
		} catch (FileNotFoundException e) {
			System.err.println("Cannot open repository, files missing");
		} catch (IOException e) {
			System.out.println("File missing");
		}
		System.out.println("Reading succesfull!");
	}

	/**
	 * Creates the folder (= initializes)
	 */
	private void createFolder() {
		System.out.println("Folder did not exits, creating...");
		try {
			for (String fol : foldersToCreate) {
				(new File(path + fol)).mkdir();
			}
			for (String fil : filesToCreate) {
				(new File(path + foldername + fil)).createNewFile();
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
		System.out.println("Creating succesfull!");
	}



	/**
	 * Writes the file that contains a list of files and id's
	 */
	protected void writeFilesFile() {
		BufferedWriter output;
		try {
			output = new BufferedWriter(new FileWriter(path + foldername
					+ filesfilename, false));
			for (Map.Entry<File, UUID> f : files.entrySet()) {
				
				// Commits can be null in local clients
				if(f.getValue() != null)
					output.append(f.getKey().getAbsolutePath() + filesFileSplitter
						+ f.getValue().toString());
				else
					output.append(f.getKey().getAbsolutePath() + filesFileSplitter
							+ noCommitString);
				
				output.newLine();
			}
			output.close();
		} catch (IOException e) {
			System.err.println("Error: Opening file file");
		}
	}

}

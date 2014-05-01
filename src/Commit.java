import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import java.util.UUID;

public class Commit {

	private String message;
	private Date time;
	private ArrayList<File> files;
	private UUID id; // see 'man uuid' for more information
	private static final String formStr = "yyyy.MM.dd/HH:mm:ss";
	private static final SimpleDateFormat form = new SimpleDateFormat(formStr);
	public static final int lines = 5;
	
	
	public Commit(String m, Date t, UUID id) {
		this.message = m;
		this.time = t;
		this.files = new ArrayList<File>();
		if (id == null) {
			this.id = UUID.randomUUID();
		} else {
			this.id = id;
		}
	}

	public void addFile(File f) {
		if (f.exists()) {
			files.add(f);
		} else {
			System.out.println("File " + f.getName() + " does not exists!");
		}
	}

	public ArrayList<File> getFiles() {
		return files;
	}

	public void readFromString(ArrayList<String> strings) {
		try {
			this.time = form.parse(strings.get(0));
			this.message = strings.get(1);
			this.id = UUID.fromString(strings.get(2));
			
			
			for(String fil : strings.subList(3, strings.size())){
				addFile(new File(fil));
			}
		} catch (ParseException e) {
			System.err.println("Could not parse Time of commit");
		}
	}

	public String writeToString() {
		String ret = "";
		ret += ":" + form.format(time) + "\n:" + message + "\n:" + id.toString()
				+ "\n:";
		if (files != null) {
			for (File f : files) {
				ret += f.getPath() + "\n:"; //we use colon because file paths can have whitespace
			}
		}
		return ret + "\nEND";
	}
}

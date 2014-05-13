import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

public class Commit {

	private String message;
	private Date time;
	private ArrayList<String> files; // relative paths in the directory
	private UUID id; // see 'man uuid' for more information
	private static final String formStr = "yyyy.MM.dd/HH:mm:ss";
	private static final SimpleDateFormat form = new SimpleDateFormat(formStr);
	public static final int lines = 5;

	public Commit(String m, Date t, UUID id) {

		this.message = m;
		this.time = t;
		this.files = new ArrayList<String>();
		if (id == null) {
			this.id = UUID.randomUUID();
		} else {
			this.id = id;
		}
	}

	/**
	 * @param f
	 *            whether this file exists should be checked before added
	 */
	public void addFile(String f) {

		files.add(f);
	}

	public String getMessage() {

		return message;
	}

	public Date getTime() {

		return time;
	}

	public ArrayList<String> getFiles() {

		return files;
	}

	/**
	 * The order where files are written and read should be the same to guarantee consistency
	 * 
	 * @param strings
	 */
	public void readFromString(ArrayList<String> strings) {

		try {
			// We have to remove the : placed in writeToString
			this.time = form.parse(strings.get(0).substring(1));
			this.message = strings.get(1).substring(1);
			this.id = UUID.fromString(strings.get(2).substring(1));

			for (String fil : strings.subList(3, strings.size() - 1)) {
				addFile(fil.substring(1));
			}
		} catch (ParseException e) {
			System.err.println("Could not parse Time of commit");
		}
	}

	public UUID getId() {

		return id;
	}

	public String writeToString() {

		String ret = "";
		ret += ":" + form.format(time) + "\n:" + message + "\n:" + id.toString() + "\n:";
		if (files != null) {
			for (String f : files) {
				ret += f + "\n:"; // we use newlines because file paths can have whitespace
			}
		}
		return ret.substring(0, ret.length() - 2) + "\nEND";
	}
}

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

public class Commit implements Writeable {

	private String message;
	private Date time;
	private ArrayList<File> files;
	private UUID id; // see man uuid for more information
	private static final String formStr = "yyyy.MM.dd/HH:mm:ss";
	private static final SimpleDateFormat form = new SimpleDateFormat(formStr);

	public Commit(String m, Date t, UUID id) {
		this.message = m;
		this.time = t;
		this.files = new ArrayList<File>();
		if (id == null) {
			this.id = UUID.randomUUID();
		}else{
			this.id = id;
		}
	}

	public void addFile(File f) {
		files.add(f);
	}

	@Override
	public String writeToString() {
		String ret = "";
		ret += form.format(time) + " \"" + message + "\" " + id.toString();
		if (files != null) {
			for (File f : files) {
				ret += f.getPath() + f.getName();
			}
		}
		return ret;
	}

	@Override
	public void readFromString(String str) {
		try {
			this.time = form.parse(str.substring(0, formStr.length()));
			this.message = str.substring(str.indexOf("\"") + 1,
					str.lastIndexOf("\"") - 1);
			this.id = UUID.fromString(str.substring(str.lastIndexOf("\"" + 1)));
		} catch (ParseException e) {
			System.err.println("Wrong commit string: " + str);
		}
	}
}

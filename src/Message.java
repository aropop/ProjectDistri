import java.io.Serializable;


public class Message implements Serializable {
	

	private static final long serialVersionUID = 1L;

	public static enum Type{
		ERROR,
		SUCCES,
		INFO,
		FILEREQUEST,
		HEARTBEAT
	}
	
	private String content;
	private Type type;
	private String splitter;
	
	public Message (String content, Type type, String split){
		this.content = content;
		this.type = type;
		this.splitter = split;
	}

	public String getContent() {
		return content;
	}

	public Type getType() {
		return type;
	}
	
	public String[] getContentArray(){
		if(splitter == null && content.length() != 0)
			return new String[] { content };
		else if (splitter == null)
			return new String[] {};
			
		return content.split(splitter);
	}
	
	
}

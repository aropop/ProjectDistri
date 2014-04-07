import java.io.Serializable;


public class Message implements Serializable {
	

	public static enum Type {
		Error, Succes, Info, FileRequest
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
		return content.split(splitter);
	}
	
	
}

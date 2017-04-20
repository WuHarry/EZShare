package exceptions;

public class InvalidServerException extends Exception{

	private static final long serialVersionUID = -641896925059277211L;

	public InvalidServerException(String message){
		super(message);
	}
	
}

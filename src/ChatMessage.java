import java.io.*;
/*
 * This class defines the different type of messages that will be exchanged between the
 * Clients and the Server. 
 * When talking from a Java Client to a Java Server a lot easier to pass Java objects, no 
 * need to count bytes or to wait for a line feed at the end of the frame
 */

public class ChatMessage implements Serializable
{
	// The different types of message sent by the Client
	// WHOISIN to receive the list of the users connected
	// REQUEST to request resources from the server
	// VIEW to view the currently  avaialble resources
	// RELEASE to relase the currently allocated resources
	// GETLOG to get the log file
	// LOGOUT to disconnect from the Server
	static final int WHOISIN = 0, REQUEST = 1, LOGOUT = 2, VIEW = 3, RELEASE = 4, GETLOG = 5;
	private int type;
	private String message;
	
	// constructor
	ChatMessage(int type, String message)
	{
		this.type = type;
		this.message = message;
	}
	int getType() 
	{
		return type;
	}
	String getMessage() 
	{
		return message;
	}
}

import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.*;
import java.nio.file.*;

//The Client that can be run as a console
public class Client  
{	
	// notification
	private String notif = " *** ";

	// for I/O
	private ObjectInputStream sInput;		// to read from the socket
	private ObjectOutputStream sOutput;		// to write on the socket
	private Socket socket;					// socket object
	
	private String server, username;	// server and username
	private int port;					//port

	public String getUsername() 
	{
		return username;
	}
	public void setUsername(String username) 
	{
		this.username = username;
	}

	/*
	 *  Constructor to set below things
	 *  server: the server address
	 *  port: the port number
	 *  username: the username
	 */
	
	Client(String server, int port, String username) 
	{
		this.server = server;
		this.port = port;
		this.username = username;
	}
	
	/*
	 * To start the chat
	 */
	public boolean start() 
	{
		// try to connect to the server
		try 
		{
			socket = new Socket(server, port);
		} 
		// exception handler if it failed
		catch(Exception ec) 
		{
			display("Error connectiong to server:" + ec);
			return false;
		}
		
		String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
		display(msg);
	
		/* Creating both Data Stream */
		try
		{
			sInput  = new ObjectInputStream(socket.getInputStream());
			sOutput = new ObjectOutputStream(socket.getOutputStream());
		}
		catch (IOException eIO) 
		{
			display("Exception creating new Input/output Streams: " + eIO);
			return false;
		}

		// creates the Thread to listen from the server 
		new ListenFromServer().start();

		// Send our username to the server this is the only message that we
		// will send as a String. All other messages will be ChatMessage objects
		try
		{
			sOutput.writeObject(username);
		}
		catch (IOException eIO) 
		{
			display("Exception doing login : " + eIO);
			disconnect();
			return false;
		}
		// success we inform the caller that it worked
		return true;
	}

	/*
	 * To send a message to the console
	 */
	private void display(String msg) 
	{
		System.out.println(msg);	
	}
	
	/*
	 * To send a message to the server
	 */
	void sendMessage(ChatMessage msg) 
	{
		try 
		{
			sOutput.writeObject(msg);
		}
		catch(IOException e) 
		{
			display("Exception writing to server: " + e);
		}
	}

	/*
	 * When something goes wrong
	 * Close the Input/Output streams and disconnect
	 */
	private void disconnect() 
	{
		try 
		{ 
			if(sInput != null) sInput.close();
		}
		catch(Exception e) 
		{}
		try 
		{
			if(sOutput != null) sOutput.close();
		}
		catch(Exception e) 
		{}
        try
        {
			if(socket != null) socket.close();
		}
		catch(Exception e) 
		{}	
	}

	/*
	 * To start the Client in console mode use one of the following command
	 * > java Client
	 * > java Client username
	 * > java Client username portNumber
	 * > java Client username portNumber serverAddress
	 * at the console prompt
	 * If the portNumber is not specified 1500 is used
	 * If the serverAddress is not specified "localHost" is used
	 * If the username is not specified "Anonymous" is used
	 */
	public static void main(String[] args) 
	{
		// default values if not entered
		int portNumber = 1500;
		String serverAddress = "localhost";
		String userName = "Anonymous";
		Scanner scan = new Scanner(System.in);
		
		System.out.println("Enter the username: ");
		userName = scan.nextLine();

		// different case according to the length of the arguments.
		switch(args.length) {
			case 3:
				// for > javac Client username portNumber serverAddr
				serverAddress = args[2];
			case 2:
				// for > javac Client username portNumber
				try {
					portNumber = Integer.parseInt(args[1]);
				}
				catch(Exception e) {
					System.out.println("Invalid port number.");
					System.out.println("Usage is: > java Client [username] [portNumber] [serverAddress]");
					return;
				}
			case 1: 
				// for > javac Client username
				userName = args[0];
			case 0:
				// for > java Client
				break;
			// if number of arguments are invalid
			default:
				System.out.println("Usage is: > java Client [username] [portNumber] [serverAddress]");
			return;
		}

		// create the Client object
		Client client = new Client(serverAddress, portNumber, userName);
		
		// try to connect to the server and return if not connected
		if(!client.start())
			return;
		
		System.out.println("\nHello.! You are connected to the Server Now.");
		System.out.println("Instructions:");
		System.out.println("1. Type 'VIEW' to view all the resources available");
		System.out.println("2. Type 'REQUEST' to request for resources (eg > REQUEST:23:45:33:45");
		System.out.println("3. Type 'RELEASE' to release allocated resources available");
		System.out.println("4. Type 'GETLOG' to access the Client LOG file");
		System.out.println("5. Type 'WHOISIN' without quotes to see list of active clients");
		System.out.println("6. Type 'LOGOUT' without quotes to logoff from server");

		// infinite loop to get the input from the user
		while(true) 
		{
			
			System.out.print("> ");
			// read message from user
			String msg = scan.nextLine();
			
			// logout if message is LOGOUT
			if(msg.equalsIgnoreCase("LOGOUT")) 
			{
				client.sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
				break;
			}
			
			// message to check who are present in chatroom
			else if(msg.equalsIgnoreCase("WHOISIN")) 
			{
				
				client.sendMessage(new ChatMessage(ChatMessage.WHOISIN, ""));				
			}
			
			else if(msg.equalsIgnoreCase("VIEW"))
			{
				client.sendMessage(new ChatMessage(ChatMessage.VIEW, ""));
			}
			
			else if(msg.equalsIgnoreCase("RELEASE"))
			{
				client.sendMessage(new ChatMessage(ChatMessage.RELEASE, ""));
			}
			else if(msg.equalsIgnoreCase("GETLOG"))
			{
				client.sendMessage(new ChatMessage(ChatMessage.GETLOG, ""));
			} 

			// regular text message
			else 
			{
				client.sendMessage(new ChatMessage(ChatMessage.REQUEST, msg));
			}
		}
		// close resource
		scan.close();
		// client completed its job. disconnect client.
		client.disconnect();	
	}

	/*
	 * a class that waits for the message from the server
	 */
	class ListenFromServer extends Thread 
	{
		//open Log File in TextEdit on Mac OS X
		//argument can be changed for Windows
		public void openEditor(String command)
		{
			Runtime runtime = Runtime.getRuntime();
			boolean running = true;
			try 
			{
				Process p = runtime.exec(command);
				p.waitFor();
			} 
			catch (Exception e) 
			{}
			running = false;
		}

		public void recieveAndsendLog(String msg)
		{
			try
			{
				File log_file = null;
					
				//Handling GETLOG
				if(msg.equalsIgnoreCase("LOG FILE ACCESSED"))
				{
					//Log File received from the server
					String file = (String) sInput.readObject();

					//creating a new file client side
					log_file = new File(username+"_log.txt");
					
					openEditor("open -W "+log_file.getPath());
					
   					String edited  = new String(Files.readAllBytes(Paths.get(username+"_log.txt")));

					//send log file(edited) back to server
					sOutput.writeObject(edited);
				}
			}
			catch(Exception e)
			{}
		}
		public void run()
		{
			while(true) 
			{
				try 
				{
					// read the message form the input datastream
					String msg = (String) sInput.readObject();
					
					//validate the message and send back to server
					recieveAndsendLog(msg);

					// print the message
					System.out.println(msg);
					System.out.print("> ");
				}
				catch(IOException e) 
				{
					display(notif + "Server has closed the connection: " + e + notif);
					break;
				}
				catch(ClassNotFoundException e2) 
				{
				}
			}
		}
	}
}


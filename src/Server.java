
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

import java.nio.file.*;

//TODO -- ADDING LOG FILE
// the server that can be run as a console
public class Server 
{
	// a unique ID for each connection
	private static int uniqueId;

	// an ArrayList to keep the list of the Client
	private ArrayList<ClientThread> al;

	// to display time
	private SimpleDateFormat sdf;

	// the port number to listen for connection
	private int port;

	// to check if server is running
	private boolean keepGoing;

	// notification
	private String notif = " *** ";

	//Queue to maintain the requests from the processes for the CS
    static volatile Queue<String> RequestsQueue = new LinkedList<>();;
    
    //Variable to store the available resource
    static volatile Resource AvailableResource;
	
    //Global Log File of Server
    static volatile Path global_path;
	
	//constructor that receive the port to listen to for connection as parameter
	public Server(int port) 
	{
		// the port
		this.port = port;
		// to display hh:mm:ss
		sdf = new SimpleDateFormat("HH:mm:ss.SSS");
		
		// an ArrayList to keep the list of the Client
		al = new ArrayList<ClientThread>();
		
		AvailableResource = new Resource(50,50,50,50);
		global_path = Paths.get("./global_log.txt");
		try
		{
			Files.write(global_path, ("GLOBAL LOG FILE \n Available Resources : "+ AvailableResource.A +":"+ AvailableResource.B +":"+ AvailableResource.C +":"+ AvailableResource.D ).getBytes());
		}
		catch(Exception e)
		{}
		
	}
	
	public void start() 
	{
		keepGoing = true;
		//create socket server and wait for connection requests 
		try 
		{
			// the socket used by the server
			ServerSocket serverSocket = new ServerSocket(port);

			// infinite loop to wait for connections ( till server is active )
			while(keepGoing) 
			{
				display("Server waiting for Clients on port " + port + ".");
				
				// accept connection if requested from client
				Socket socket = serverSocket.accept();

				// break if server stoped
				if(!keepGoing)
					break;

				// if client is connected, create its thread
				ClientThread t = new ClientThread(socket);

				//add this client to arraylist
				al.add(t);
				t.start();
			}
			// try to stop the server
			try 
			{
				serverSocket.close();
				for(int i = 0; i < al.size(); ++i) 
				{
					ClientThread tc = al.get(i);
					try 
					{
					// close all data streams and socket
					tc.sInput.close();
					tc.sOutput.close();
					tc.socket.close();
					}
					catch(IOException ioE) {
					}
				}
			}
			catch(Exception e) 
			{
				display("Exception closing the server and clients: " + e);
			}
		}
		catch (IOException e) 
		{
            String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
			display(msg);
		}
	}
	
	// to stop the server
	protected void stop() 
	{
		keepGoing = false;
		try 
		{
			new Socket("localhost", port);
		}
		catch(Exception e) 
		{
		}
	}
	
	// Display an event to the console
	private void display(String msg) 
	{
		String time = sdf.format(new Date()) + " " + msg;
		System.out.println(time);
	}
	
	// to broadcast a message to all Clients
	private synchronized boolean broadcast(String message) 
	{
		// add timestamp to the message
		String time = sdf.format(new Date());
		String messageLf = time + " " + message + "\n";
			
		// display message
		System.out.print(messageLf);
			
		// we loop in reverse order in case we would have to remove a Client
		// because it has disconnected
		for(int i = al.size(); --i >= 0;) 
		{
			ClientThread ct = al.get(i);
			// try to write to the Client if it fails remove it from the list
			if(!ct.writeMsg(messageLf)) 
			{
				al.remove(i);
				display("Disconnected Client " + ct.username + " removed from list.");
			}
		}
		return true;
	}

	// if client sent LOGOUT message to exit
	synchronized void remove(int id) 
	{
		String disconnectedClient = "";
		// scan the array list until we found the Id
		for(int i = 0; i < al.size(); ++i) 
		{
			ClientThread ct = al.get(i);
			// if found remove it
			if(ct.id == id) {
				disconnectedClient = ct.getUsername();
				al.remove(i);
				break;
			}
		}
		broadcast(notif + disconnectedClient + " has left the connection from the server." + notif);
	}
	
	/*
	 *  To run as a console application
	 * > java Server
	 * > java Server portNumber
	 * If the port number is not specified 1500 is used
	 */ 
	public static void main(String[] args) 
	{
		// start server on port 1500 unless a PortNumber is specified 
		int portNumber = 1500;
		switch(args.length) 
		{
			case 1:
				try 
				{
					portNumber = Integer.parseInt(args[0]);
				}
				catch(Exception e) 
				{
					System.out.println("Invalid port number.");
					System.out.println("Usage is: > java Server [portNumber]");
					return;
				}
			case 0:
				break;
			default:
				System.out.println("Usage is: > java Server [portNumber]");
				return;
				
		}
		// create a server object and start it
		Server server = new Server(portNumber);
		server.start();
	}

	// One instance of this thread will run for each client
	class ClientThread extends Thread 
	{

		// the socket to get messages from client
		Socket socket;
		ObjectInputStream sInput;
		ObjectOutputStream sOutput;
		// my unique id (easier for deconnection)

		int id;


		Path path;

		// the Username of the Client
		String username;

		// message object to recieve message and its type
		ChatMessage cm;

		// timestamp
		String date;
		// Constructor
		ClientThread(Socket socket) 
		{
			// a unique id
			id = ++uniqueId;
			this.socket = socket;

			//Creating both Data Stream
			System.out.println("Thread trying to create Object Input/Output Streams");

			try
			{
				sOutput = new ObjectOutputStream(socket.getOutputStream());
				sInput  = new ObjectInputStream(socket.getInputStream());
				// read the username
				username = (String) sInput.readObject();
				//broadcast(notif + username + " has joined the room." + notif);
			}
			catch (IOException e) 
			{
				display("Exception creating new Input/output Streams: " + e);
				return;
			}
			catch (ClassNotFoundException e) 
			{
			}
			path = Paths.get("./"+username+"_log.txt");
			try
			{
				Files.write(path, (username.toUpperCase()+"'s LOG FILE \n DATE \t\t\t REQUEST \t\t STATUS \n").getBytes());
			}
			catch(Exception e)
			{}
            date = new Date().toString();

		}
		
		public String getUsername() 
		{
			return username;
		}

		public void setUsername(String username) 
		{
			this.username = username;
		}

		private synchronized int CheckAvailable(Resource request)
		{
		if(Server.AvailableResource.A >= request.A && Server.AvailableResource.B >= request.B
		        && Server.AvailableResource.C >= request.C && Server.AvailableResource.D >= request.D  )
		{
			String yes =String.format("%s - A: %d B: %d C: %d D: %d Y \n", date, request.A, request.B, request.C, request.D);
			try
            {
            	Files.write(path, yes.getBytes(), StandardOpenOption.APPEND);
            	Files.write(global_path,("\n"+username +" : "+ yes).getBytes(), StandardOpenOption.APPEND);
            }
            catch(Exception e)
            {
            }
		    return 1;
		}
			String no = String.format("%s - A: %d B: %d C: %d D: %d N \n", date, request.A, request.B, request.C, request.D);
			try
            {
            	Files.write(path, no.getBytes(), StandardOpenOption.APPEND);
            	Files.write(global_path,("\n"+username +" : "+ no).getBytes(), StandardOpenOption.APPEND);
            }
            catch(Exception e)
            {
            }
		return 0;
		}

		private synchronized void RequestGrant(Resource request)
		{
			Server.AvailableResource.A -= request.A;
			Server.AvailableResource.B -= request.B;
			Server.AvailableResource.C -= request.C;
			Server.AvailableResource.D -= request.D;
		}

		public void checkRequest(Resource request) throws IOException, InterruptedException
		{
		//check if the available instances of the resources are more than the requested
		int flag = CheckAvailable(request);

		//Put in queue if not available
		if(flag == 0)
		{
		    //Add the process to the current queue
		    Server.RequestsQueue.add(Thread.currentThread().getName());
		    System.out.println(Thread.currentThread().getName()+"added to queue");
		    while(!Server.RequestsQueue.element().equalsIgnoreCase(Thread.currentThread().getName()))
		    {
		        Thread.sleep(1000);
		    }
		    while(CheckAvailable(request)==0)
		    {
		        Thread.currentThread().sleep(1000);
		    }
		    Server.RequestsQueue.remove();
		}
		RequestGrant(request);
		writeMsg("Request has been Granted");
		}

		

		// infinite loop to read and forward message
		public void run()
		{
			// to loop until LOGOUT
			Resource request = null;
			Resource totalRequest = null;
			int rA, rB, rC, rD;
			int new_rA, new_rB, new_rC, new_rD;
			boolean keepGoing = true;
			boolean firstReq = false;
			while(keepGoing) 
			{
				// read a String (which is an object)
				try 
				{
					cm = (ChatMessage) sInput.readObject();
				}
				catch (IOException e) 
				{
					display(username + " Exception reading Streams: " + e);
					break;				
				}
				catch(ClassNotFoundException e2) 
				{
					break;
				}
				// get the message from the ChatMessage object received
				String message = cm.getMessage();
				
				// different actions based on type message
				switch(cm.getType()) 
				{
				case ChatMessage.VIEW:
					writeMsg("\n A="+ Server.AvailableResource.A+"\nB="+ Server.AvailableResource.B+"\nC="+ Server.AvailableResource.C+"\nD="+ Server.AvailableResource.D);
					break;

				case ChatMessage.GETLOG:
					
					try
					{
						String log_file = new String(Files.readAllBytes(path));
						sOutput.writeObject("LOG FILE ACCESSED");
						writeMsg(log_file);
					
						if(((String)sInput.readObject()).equalsIgnoreCase(log_file))
						{
							writeMsg("NO CHANGES MADE BY THE CLIENT");
						}
						else
						{
							
							broadcast(id +" : " + username + " IS A DAEMON CLIENT ");
							keepGoing = false;
							//recovery code
							Server.AvailableResource.A += totalRequest.A;
                        	Server.AvailableResource.B += totalRequest.B;
                        	Server.AvailableResource.C += totalRequest.C;
                        	Server.AvailableResource.D += totalRequest.D;
                        	
                        	Files.write(path,("\n DAEMON CLIENT. ALL THE RESOURCES ALLOCATED TO THIS CLIENT ARE TAKEN BACK.").getBytes(), StandardOpenOption.APPEND);
                        	Files.write(global_path,("\n"+username +" : " + "DAEMON CLIENT. ALL THE RESOURCES ALLOCATED TO THIS CLIENT ARE TAKEN BACK. CURRENT RESOURCES STATUS : "+ AvailableResource.A +":"+ AvailableResource.B +":"+ AvailableResource.C +":"+ AvailableResource.D).getBytes(), StandardOpenOption.APPEND);
						}
					}
				catch(Exception e)
				{}

					break;	

				case ChatMessage.REQUEST:
					String[] arrOfStr = message.split(":", 5);
					if(!firstReq)
		            {
		            	rA = Integer.parseInt(arrOfStr[1]);
		            	rB = Integer.parseInt(arrOfStr[2]);
		            	rC = Integer.parseInt(arrOfStr[3]);
		            	rD = Integer.parseInt(arrOfStr[4]);
		            	request = new Resource(rA , rB , rC , rD);
		            	totalRequest = new Resource(rA , rB , rC , rD);
		            	firstReq = true;
		            }
		            else
		            {
		            	new_rA = Integer.parseInt(arrOfStr[1]);
		            	new_rB = Integer.parseInt(arrOfStr[2]);
		            	new_rC = Integer.parseInt(arrOfStr[3]);
		            	new_rD = Integer.parseInt(arrOfStr[4]);
		            	request.A = new_rA;
		            	request.B = new_rB;
		            	request.C = new_rC;
		            	request.D = new_rD;
		            	totalRequest.A += new_rA;
		            	totalRequest.B += new_rB; 
		            	totalRequest.C += new_rC;
		            	totalRequest.D += new_rD;
		            }
		            try
		            {
	                	checkRequest(request);
	                }
	                catch(Exception e)
	                {
	                	break;
	                }
	                break;

	            case ChatMessage.RELEASE:
					synchronized(this)
                    {
                        Server.AvailableResource.A += totalRequest.A;
                        Server.AvailableResource.B += totalRequest.B;
                        Server.AvailableResource.C += totalRequest.C;
                        Server.AvailableResource.D += totalRequest.D;
                    }
                    totalRequest.A =0;
		            totalRequest.B =0;
		            totalRequest.C =0;
		            totalRequest.D =0;
		            writeMsg("Resources have been released successfully");
				break;
				
				case ChatMessage.LOGOUT:
					display(username + " disconnected with a LOGOUT message.");
					keepGoing = false;
					break;
				case ChatMessage.WHOISIN:
					
					writeMsg("List of the users connected at " + sdf.format(new Date()) + "\n");
					// send list of active clients
					for(int i = 0; i < al.size(); ++i) 
					{
						ClientThread ct = al.get(i);
						writeMsg((i+1) + ") " + ct.username + " since " + ct.date);
					}
					break;
				}
			}
			// if out of the loop then disconnected and remove from client list
			remove(id);
			close();
		}
		
		// close everything
		private void close() 
		{
			try 
			{
				if(sOutput != null) sOutput.close();
			}
			catch(Exception e) {}
			try 
			{
				if(sInput != null) sInput.close();
			}
			catch(Exception e) {};
			try 
			{
				if(socket != null) socket.close();
			}
			catch (Exception e) {}
		}

		// write a String to the Client output stream
		private boolean writeMsg(String msg) 
		{
			// if Client is still connected send the message to it
			if(!socket.isConnected()) 
			{
				close();
				return false;
			}
			// write the message to the stream
			try 
			{
				sOutput.writeObject(msg);
			}
			// if an error occurs, do not abort just inform the user
			catch(IOException e) 
			{
				display(notif + "Error sending message to " + username + notif);
				display(e.toString());
			}
			return true;
		}
	}
}


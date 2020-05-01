# Assignment on Error recovery:

Create distributed systems with socket programming. Create multiple- client and server environment.
Create a log-file for each client, server maintains them. The log-file of respective client contains all the requests made by it along with time stamps and the status of request processing (Y/N). A client can make multiple requests to server (you can refer previous assignments). Client can ask its log file as one of the requests. But the client has to send the log file back after reading it.

### Among all the processes there is a daemon process, which tries to corrupt log file. The server should be capable of tracing this information. After the request is processed the server can check immediately or at regular intervals of time for the following things and correct them:
1. If the client corrupts the log file, the server should name the client as daemon and notify other clients.
2. The system has to recover from this.
3. Use any error recovery approaches to go back to the previous points. (Maintain a log for this)
4. Daemon process should be blocked.

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

//TODO: Add Heartbeat function

public class Server {

    private static int uniqueId;
    private int port;
    private SimpleDateFormat sdf;
    private boolean keepGoing;
    private ArrayList<ClientThread> clientList;
    private String stars = " *** ";

    public Server(int port) {
        // the port
        this.port = port;
        // to display hh:mm:ss
        sdf = new SimpleDateFormat("HH:mm:ss");
        // an ArrayList to keep the list of the Client
        clientList = new ArrayList<>();
    }

    public Server(){
    }

    public void start() {
        keepGoing = true;
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            display("Server waiting for Clients on port: " + port + " ...");
            while (keepGoing) {
                Socket socket = serverSocket.accept();

                ClientThread clientThread = new ClientThread(socket);

                if (!keepGoing) {
                    break;
                }
                //Add the client to the Arraylist
                clientList.add(clientThread);

                clientThread.start();
            }
            try {
                serverSocket.close();
                for (ClientThread clientThread : clientList) {
                    try {
                        // close all data streams and socket
                        clientThread.sInput.close();
                        clientThread.sOutput.close();
                        clientThread.socket.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                display("Exception closing the server and clients: " + e);
            }
        } catch (IOException e) {
            String message = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
            display(message);
        }
    }

    public synchronized void broadcast(String message){
        String time = sdf.format(new Date());

        String messageLf = time + " " + message + "\n";
        // display message
        System.out.print(messageLf);

        //Prints out the message for all clients in list
        for (ClientThread clientThread : clientList) {
            clientThread.writeMsg(messageLf);
        }
    }

    public synchronized void remove(int id) {
        String disconnectedClient="";
        // scan the array list until we found the Id
        //In reverse order
        for (int i = 0; i < clientList.size(); ++i) {
            ClientThread clientThread = clientList.get(i);
            // if found remove it
            if (clientThread.id == id) {
                disconnectedClient = clientThread.getUsername();
                clientList.remove(i);
                break;
            }
        }
        broadcast(stars + disconnectedClient + " has left the chat room." + stars);
    }

    public void display(String message) {
        String time = sdf.format(new Date()) + " " + message;
        System.out.println(time);
    }

    public static void main(String[] args) {
        int portNumber = 1337;

        Server server = new Server(portNumber);
        server.start();
    }

    //Inner class
    public class ClientThread extends Thread {
        // the socket to get messages from client
        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        // my unique id (easier for deconnection)
        int id;

        // the Username of the Client
        String username;
        // message object to recieve message and its type
        ChatMessage cm;
        // timestamp
        String date;

        public ClientThread(Socket socket) {
            //a unique id
            id = ++uniqueId;
            this.socket = socket;
            //Creating both Data Stream
            //Thread trying to create Object Input/Output Streams
            try {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput  = new ObjectInputStream(socket.getInputStream());
                // read the username
                username = (String) sInput.readObject();

                broadcast(stars + username + " has joined the chat room." + stars);
            }
            catch (IOException e) {
                display("Exception creating new Input/output Streams: " + e);
                return;
            }
            catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            date = new Date().toString() + "\n";
        }

        public ClientThread(){
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        // infinite loop to read and forward message
        public void run() {
            // to loop until LOGOUT
            boolean keepGoing = true;
            while(keepGoing) {
                // read a String (which is an object)
                try {
                    cm = (ChatMessage) sInput.readObject();
                }
                catch (IOException e) {
                    display(username + " Exception reading Streams: " + e);
                    break;
                }
                catch(ClassNotFoundException e) {
                    e.printStackTrace();
                    break;
                }
                // get the message from the ChatMessage object received
                String message = cm.getMessage();

                // different actions based on type message
                switch(cm.getType()) {
                    case ChatMessage.MESSAGE:
                        broadcast(username + ": " + message);
                        break;
                    case ChatMessage.QUIT:
                        display(username + " has disconnected.");
                        keepGoing = false;
                        break;
                    case ChatMessage.LIST:
                        writeMsg("List of the users connected at " + sdf.format(new Date()) + "\n");
                        // send list of active clients
                        for(int i = 0; i < clientList.size(); ++i) {
                            ClientThread clientThread = clientList.get(i);
                            writeMsg((i+1) + ") " + clientThread.username + " since " + clientThread.date);
                        }
                        break;
                }
            }
            // if out of the loop then disconnected and remove from client list
            remove(id);
            close();
        }

        // close everything
        public void close() {
            try {
                if(sOutput != null) sOutput.close();
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            try {
                if(sInput != null) sInput.close();
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            try {
                if(socket != null) socket.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        // write a String to the Client output stream
        public void writeMsg(String message) {
            // if Client is still connected send the message to it
            if(!socket.isConnected()) {
                close();
            }
            // write the message to the stream
            try {
                sOutput.writeObject(message);
            }
            // if an error occurs, do not abort just inform the user
            catch(IOException e) {
                display(stars + "Error sending message to " + username + stars);
                display(e.toString());
            }
        }
    }
}
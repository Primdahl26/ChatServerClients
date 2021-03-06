import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

//TODO: Add Heartbeat function

public class Server {

    private static int uniqueId;
    private int port;
    private SimpleDateFormat sdf;
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

    public void start() {
        boolean keepGoing = true;
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

        String messageAll = time + " " + message + "\n";
        // display message
        System.out.print(messageAll);

        //Prints out the message for all clients in list
        for (ClientThread clientThread : clientList) {
            clientThread.writeMessage(messageAll);
        }
    }

    public synchronized void remove(int id) {
        String disconnectedClient="";
        //Scan the array list until we found the Id
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
        String username;
        //Message object to recieve message and its type
        ChatMessage cm;
        //Timestamp
        String date;
        //Unique id to disconnect
        int id;

        public ClientThread(Socket socket) {
            //a unique id
            id = ++uniqueId;
            this.socket = socket;

                try {
                    //Creating both Data Streams
                    //Thread trying to create Object Input/Output Streams
                    sOutput = new ObjectOutputStream(socket.getOutputStream());
                    sInput = new ObjectInputStream(socket.getInputStream());

                    //Read the username
                    username = (String) sInput.readObject();

                    //TODO: Send a message to client that username is already in use
                    //TODO: Use the value from usernameTaken()
                    //If the username is taken
                    if(usernameTaken()){

                    }

                } catch (IOException IOe) {
                    display("Exception creating new Input/output Streams: " + IOe);
                    return;
                } catch (ClassNotFoundException CNFe) {
                    CNFe.printStackTrace();
                }

            date = new Date().toString() + "\n";

            broadcast(stars + username + " has joined the chat room." + stars);
            showClientList();
        }

        public String getUsername() {
            return username;
        }

        //Infinite loop to read and forward message
        public void run() {
            //To loop until QUIT
            boolean keepGoing = true;
            while(keepGoing) {
                //Read a String (which is an object)
                try {
                    cm = (ChatMessage) sInput.readObject();
                }
                catch (IOException e) {
                    display(username + " Exception reading Streams: " + e);
                    break;
                }
                catch(ClassNotFoundException CNFe) {
                    CNFe.printStackTrace();
                    break;
                }
                //Get the message from the ChatMessage object received
                String message = cm.getMessage();

                //Different actions based on type message
                switch(cm.getType()) {
                    case ChatMessage.MESSAGE:
                        broadcast(username + ": " + message);
                        break;
                    case ChatMessage.QUIT:
                        display(username + " has disconnected.");
                        keepGoing = false;
                        break;
                    case ChatMessage.LIST:
                        showClientList();
                        break;
                }
            }
            //If out of the loop then disconnected and remove from client list
            remove(id);
            close();
        }

        //Close everything
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

        //Method that checks if the username is taken or not
        public boolean usernameTaken(){
            boolean usernameTaken = false;
            //Only run the loop if there is anything in the list
            if (clientList.size() != 0) {
                //Loops to check if the username exists in the list
                for (ClientThread clientThread : clientList) {
                    if (clientThread.getUsername().equalsIgnoreCase(username)) {
                        usernameTaken = true;
                        System.out.println("Username already exists: " + clientThread.getUsername());
                    }
                }
            }
            return usernameTaken;
        }

        //Shows a list of active clients
        public void showClientList(){
            //Shows the list of users online
            //But only if there is 1 or more users online
            if (clientList.size()!=0) {
                writeMessage("List of the users connected at " + sdf.format(new Date())+":");
                //Send list of active clients
                for (int i = 0; i < clientList.size(); ++i) {
                    ClientThread clientThread = clientList.get(i);
                    writeMessage((i + 1) + ") " + clientThread.username + " since " + clientThread.date);
                }
            } else{
                writeMessage("You are the only one connected at " + sdf.format(new Date())+"\n");
            }
        }

        //Write a String to the Client output stream
        public void writeMessage(String message) {
            //If Client is still connected send the message to it
            if(!socket.isConnected()) {
                close();
            }
            //Write the message to the stream
            try {
                sOutput.writeObject(message);
            }
            //If an error occurs, do not abort just inform the user
            catch(IOException IOe) {
                IOe.printStackTrace();
            }
        }
    }
}
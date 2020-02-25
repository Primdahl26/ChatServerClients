import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

//TODO: Make 2 users with same name unable to connect
//TODO: Optional - Add a log feature

public class Client {

    private String stars = " *** ";

    // for I/O
    // to read from the socket
    private ObjectInputStream sInput;
    // to write on the socket
    private ObjectOutputStream sOutput;

    private Socket socket;

    private String server, username;
    private int port;

    public Client(String server, int port, String username) {
        this.server = server;
        this.port = port;
        this.username = username;
    }

    public boolean start() {
        // try to connect to the server
        try {
            socket = new Socket(server, port);
        }
        // exception handler if it failed
        catch(Exception ec) {
            display("Error connectiong to server:" + ec);
            return false;
        }

        String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
        display(msg);

        // Creating both Data Stream
        try {
            sInput  = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        }
        catch (IOException eIO) {
            display("Exception creating new Input/output Streams: " + eIO);
            return false;
        }

        // creates the Thread to listen from the server
        new ListenFromServer().start();
        // Send our username to the server this is the only message that we
        // will send as a String. All other messages will be ChatMessage objects
        try {
            sOutput.writeObject(username);
        }
        catch (IOException eIO) {
            display("Exception doing login : " + eIO);
            disconnect();
            return false;
        }
        // success we inform the caller that it worked
        return true;
    }

    public void display(String msg) {
        System.out.println(msg);
    }

    public void sendMessage(ChatMessage msg) {
        try {
            sOutput.writeObject(msg);
        }
        catch(IOException e) {
            display("Exception writing to server: " + e);
        }
    }

    public void disconnect() {
        try {
            if(sInput != null) sInput.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        try {
            if(sOutput != null) sOutput.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        try{
            if(socket != null) socket.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // default values if not entered
        int portNumber = 1337;
        String serverAddress = "localhost";
        String userName;
        Scanner scan = new Scanner(System.in);

        System.out.print("Enter the username: ");
        userName = scan.nextLine();

        // create the Client object
        Client client = new Client(serverAddress, portNumber, userName);
        // try to connect to the server and return if not connected
        if(!client.start())
            return;

        System.out.println("\nHello "+userName+"! Welcome to the chatroom.");
        System.out.println("Instructions:");
        System.out.println("1. Simply type the message to send broadcast to all active clients");
        System.out.println("2. Type LIST to see list of active clients");
        System.out.println("3. Type QUIT to disconnect from server");

        // infinite loop to get the input from the user
        while(true) {
            System.out.print("> ");
            // read message from user
            String msg = scan.nextLine();
            // logout if message is LOGOUT
            if(msg.equalsIgnoreCase("QUIT")) {
                client.sendMessage(new ChatMessage(ChatMessage.QUIT, ""));
                break;
            }
            // message to check who are present in chatroom
            else if(msg.equalsIgnoreCase("LIST")) {
                client.sendMessage(new ChatMessage(ChatMessage.LIST, ""));
            }
            // regular text message
            else {
                client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, msg));
            }
        }
        // close resource
        scan.close();
        // client completed its job. disconnect client.
        client.disconnect();
    }

    //Nested class
    class ListenFromServer extends Thread {

        public void run() {
            while(true) {
                try {
                    // read the message form the input datastream
                    String msg = (String) sInput.readObject();
                    // print the message
                    System.out.println(msg);
                    System.out.print("> ");
                }
                catch(IOException e) {
                    display(stars + "Server has closed the connection: " + e + stars);
                    break;
                }
                catch(ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
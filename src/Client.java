import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

//TODO: Make 2 users with same name unable to connect

public class Client {

    private ObjectInputStream sInput;
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
        catch(Exception e) {
            display("Error connectiong to server:" + e);
            return false;
        }

        String message = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
        display(message);

        //Creating both Data Stream
        try {
            sInput  = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());

        }
        catch (IOException IOe) {
            display("Exception creating new Input/output Streams: " + IOe);
            return false;
        }

        //creates the Thread to listen from the server
        new ListenFromServer().start();

        //Send our username to the server
        try {
            sOutput.writeObject(username);

        } catch (IOException IOe) {
            display("Exception doing login : " + IOe);
            disconnect();
            return false;
        }
        //Everything succeeded
        return true;
    }

    public void display(String message) {
        System.out.println(message);
    }

        public void sendMessage(ChatMessage message) {
        try {
            sOutput.writeObject(message);
        }
        catch(IOException e) {
            display("Exception writing to server: " + e);
        }
    }

    //Method to close all connections
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

    //TODO: Find a way to set this in a loop
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
        if(!client.start()) {
            return;
        }

        System.out.println("\nHello "+userName+"! Welcome to the chatroom.\n");
        System.out.println("Instructions:");
        System.out.println("1. Simply type the message to send broadcast to all active clients");
        System.out.println("2. Type LIST to see list of active clients");
        System.out.println("3. Type QUIT to disconnect from server\n");

        // infinite loop to get the input from the user
        while(true) {
            System.out.print("> ");
            // read message from user
            String message = scan.nextLine();
            // Quit of message is quit
            if(message.equals("QUIT")) {
                client.sendMessage(new ChatMessage(ChatMessage.QUIT, ""));
                //Break the loop
                break;
            }
            // message to check who are present in chatroom
            else if(message.equals("LIST")) {
                client.sendMessage(new ChatMessage(ChatMessage.LIST, ""));
            }
            // regular text message
            else {
                client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, message));
            }
        }
        //When out of the while loop (QUIT is typed)
        scan.close();
        client.disconnect();
    }

    //Inner class
    class ListenFromServer extends Thread {

        public void run() {
            while(true) {
                try {
                    // read the message form the input datastream
                    String message = (String) sInput.readObject();
                    // print the message
                    System.out.println(message);
                    System.out.print("> ");
                }
                catch(IOException e) {
                    String stars = " *** ";
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
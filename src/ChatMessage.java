import java.io.Serializable;

public class ChatMessage implements Serializable {

    // The different types of message sent by the Client
    // LIST to receive the list of the users connected
    // MESSAGE an ordinary text message
    // QUIT to disconnect from the Server
    static final int LIST = 0, MESSAGE = 1, QUIT = 2;
    private int type;
    private String message;

    ChatMessage(int type, String message) {
        this.type = type;
        this.message = message;
    }

    int getType() {
        return type;
    }

    String getMessage() {
        return message;
    }
}
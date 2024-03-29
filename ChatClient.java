import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * <h1>Launcher for Chat Client</h1>
 * This launcher will start the chat client and connect to a server.
 * <p>
 * The client supports simultaneous processing of sending and receiving
 * messages.
 */
public class ChatClient {

    /**
     * Store the socket that the client is connected on.
     */
    private Socket socket;

    /**
     * Constructor. Connects the client to the server on the specified IP
     * address and port.
     *
     * @param address
     *      The IP address to attempt a connection on.
     *
     * @param port
     *      The port to attempt a connection on.
     */
    private ChatClient(String address, int port) {
        try {
            socket = new Socket(address, port);

        } catch (UnknownHostException e) {
            System.err.println("IP address could not be determined.");
            System.exit(-1);

        } catch (ConnectException e) {
            System.err.println("Could not connect.");
            System.exit(-1);

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Starts a new thread for receiving user input and sending to the server
     * and a new thread for receiving from the server.
     *
     * @param usingGUI
     *      Whether the GUI is being used.
     */
    private void begin(boolean usingGUI) {
        try {
            BufferedReader socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter socketOut = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in));

            if (usingGUI) {
                Thread GUI = new Thread(() -> ClientGUI.begin(socket, socketIn, socketOut));
                GUI.start();

            } else {
                new Thread(new ClientSend(socket, userIn, socketOut)).start();
                new Thread(new ClientReceive(socket, socketIn)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This is the main method that checks for a new port number and IP address
     * if specified on the command line at launch.
     *
     * @param args
     *      Used to change the port number if -ccp flag is entered and the IP
     *      address if -cca flag is entered. Will also launch the GUI if the
     *      -gui flag is entered.
     */
    public static void main(String[] args) {
        boolean launchGUI = false;
        String address = "localhost";
        int portNumber = 14001;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-cca":
                    if (i + 1 < args.length) {
                        address = args[i + 1];
                    } else {
                        System.err.println("No address given.");
                        System.exit(-1);
                    }
                    break;

                case "-ccp":
                    if (i + 1 < args.length) {
                        try {
                            portNumber = Integer.parseInt(args[i + 1]);

                        } catch (NumberFormatException e) {
                            System.err.println("Error in port number.");
                            System.exit(-1);
                        }
                    } else {
                        System.err.println("No port number given.");
                        System.exit(-1);
                    }
                    break;

                case "-gui":
                    launchGUI = true;
                    break;
            }
        }
        System.out.println("Connecting to server on : " + address + " ; " + portNumber);
        new ChatClient(address, portNumber).begin(launchGUI);
    }
}

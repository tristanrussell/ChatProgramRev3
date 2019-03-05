import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

class ClientThread extends MsgControl {

    private boolean shutdown = false;

    ClientThread(Socket socket) {
        super(socket);
    }

    @Override
    void setName() {
        try {
            String testName;
            String message;
            while (true) {
                out.println("Please enter a username (max 15 characters):");
                message = in.readLine();
                String[] parts = message.split("~", 2);

                if (parts.length == 2 && parts[0].equals("2")) {
                    testName = parts[1];

                    if (testName.length() <= 15) {
                        if (testName.matches("(.*)[~<>:;](.*)")) {
                            out.println("Name must not contain ~ < > : ;");

                        } else {
                            if (clients.containsKey(testName)) {
                                out.println("Name already in use.");

                            } else {
                                name = testName;
                                clients.put(name, out);
                                out.println("READY:" + name);
                                break;
                            }
                        }
                    }
                } else {
                    out.println("Invalid name.");
                }
            }
        } catch (SocketException e) {
            if (!endThread()) {
                System.err.println("Connection to socket lost.");
                shutdown = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    boolean endThread() {
        return socket.isClosed() || shutdown;
    }

    @Override
    void processMsg(String message) {
        String[] parts = message.split("~", 2);
        String identifier;
        if (parts.length == 2) {
            identifier = parts[0];

        } else {
            identifier = "-1";
        }

        switch (identifier) {
            case "-1":
                brokenMsg();
                break;

            case "0":
                try {
                    socket.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case "1":
                msgServer(parts[1]);
                break;

            case "2":
                msgAll(name, parts[1]);
                break;

            case "3":
                msgDirect(name, parts[1]);
                break;

            case "4":
                getClientsList();
                break;

            case "5":
                kick(name, parts[1]);
                break;

            default:
                brokenMsg();
        }
    }

    @Override
    void exit() {
        try {
            if (name != null) {
                clients.remove(name);
            }
            socket.close();
            serverWriter.println("Client has disconnected on " + socket.getLocalPort() + " : " + socket.getPort());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    void brokenMsg() {
        if (++brokenMsgCount > 3) {
            String returnMsg = msgBuilder(0, "server", "Weak connection to server, disconnecting...");
            out.println(returnMsg);
            exit();

        } else {
            String returnMsg = msgBuilder(1, "server", "We could not process your last message.");
            out.println(returnMsg);
        }
    }

    @Override
    void msgServer(String message) {
        if (admins.contains(name)) {
            serverWriter.println(name + " > " + message);

        } else {
            String returnMsg = msgBuilder(1, "server", "You do not have permission to do that.");
            out.println(returnMsg);
        }
    }

    @Override
    void msgAll(String name, String message) {
        String messageOut = msgBuilder(2, name, message);

        for (String client : clients.keySet()) {
            if (!client.equals(name)) {
                clients.get(client).println(messageOut);
            }
        }
    }

    // message will have format (toUser)>(message)
    @Override
    void msgDirect(String name, String message) {
        String[] parts = message.split(":", 2);
        if (parts.length == 2) {
            if (clients.containsKey(parts[0])) {
                String messageOut = msgBuilder(3, name, parts[1]);
                clients.get(parts[0]).println(messageOut);

            } else {
                String messageOut = msgBuilder(1, "server", "There is not a user with this name.");
                out.println(messageOut);
            }
        } else {
            brokenMsg();
        }
    }

    @Override
    void getClientsList() {
        String list = "";
        int i = 1;
        for (String client : clients.keySet()) {
            list = list.concat("\t" + client);

            if (++i % 4 == 0) {
                list = list.concat("\n");
            }
        }
        String messageOut = msgBuilder(4, "null", list);
        out.println(messageOut);
    }

    @Override
    void kick(String kickedBy, String toKick) {
        if (admins.contains(name)) {
            if (clients.containsKey(toKick)) {
                if (admins.contains(toKick)) {
                    String messageOut = msgBuilder(1, "server", "Only the server can kick admins.");
                    out.println(messageOut);

                } else {
                    PrintWriter kickClient = clients.remove(toKick);
                    String removeMsg = msgBuilder(5, "server", "null");
                    kickClient.println(removeMsg);
                    kickModify("Add", toKick);
                }
            } else {
                String messageOut = msgBuilder(1, "server", "This user does not exist.");
                out.println(messageOut);
            }
        } else {
            String messageOut = msgBuilder(1, "server", "You do not have permission to do that.");
            out.println(messageOut);
        }
    }
}

package com.nedap.university.server;

import java.net.DatagramSocket;
import java.util.Scanner;

/**
 * Represents the handler of the connected client for the server.
 */
public class ClientHandler {

    /**
     * Creates
     *
     * @param socket
     * @param server
     */
    public ClientHandler(DatagramSocket socket, Server server) {
//        this.socket = socket;
//        this.server = server;
    }

    public void start() { // todo
        // socket??
        boolean connected = true;
        while (connected) {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Type what you want to do:");
            String input = scanner.nextLine();
            String[] split = input.split("\\s+");
            String command = split[0];
//            String param = null;
            switch (command) {
                case UPLOAD:
                    // todo
                    break;
                case DOWNLOAD:
                    // todo
                    break;
                case REMOVE:
                    // todo
                    break;
                case REPLACE:
                    // todo
                    break;
                case LIST:
                    // todo
                    break;
            }
        }
    }
}

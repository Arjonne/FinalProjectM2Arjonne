package com.nedap.university.server;

import java.net.DatagramSocket;
import java.sql.SQLOutput;
import java.util.Scanner;

/**
 * Represents the handler of the connected client for the server.
 */
public class ClientHandler {
    private Server server;
    private DatagramSocket clientSocket;
    private final String UPLOAD = "UPLOAD";
    private final String DOWNLOAD = "DOWNLOAD";
    private final String REMOVE = "REMOVE";
    private final String REPLACE = "REPLACE";
    private final String LIST = "LIST";
    private final String CLOSE = "CLOSE";


    /**
     * Create a clientHandler to be able to handle the input from the client that is connected to the server on the PI.
     *
     * @param clientSocket is the client socket that is needed to establish a connection between server and client.
     * @param server is the server a client is connected to.
     */
    public ClientHandler(DatagramSocket clientSocket, Server server) {
        this.clientSocket = clientSocket;
        this.server = server;
    }

    public void start() {
        boolean connected = true;
        while (connected) {
            String input = //todo;
            String[] split = input.split("\\s+");
            String command = split[0];
            String fileName = null;
            String oldFileName = null;
            String newFileName = null;
            if (split.length == 2) {
                fileName = split[1];
            } else if (split.length > 2) {
                oldFileName = split[1];
                newFileName = split[2];
            }
            switch (command) {
                case UPLOAD:
                    server.receiveFile(fileName);
                    break;
                case DOWNLOAD:
                    server.sendFile(fileName);
                    break;
                case REMOVE:
                    server.removeFile(fileName);
                    break;
                case REPLACE:
                    server.replaceFile(oldFileName, newFileName);
                    break;
                case LIST:
                    server.listFiles();
                    break;
                case CLOSE:
                    server.stop();
                    System.out.println("Application is closing.");
                default:
                    server.respondToUnknownRequest();
                    break;
            }
        }
    }
}

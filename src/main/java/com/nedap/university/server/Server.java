package com.nedap.university.server;

import com.nedap.university.Protocol;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the server on the Raspberry Pi.
 */

public class Server {
    private int port;
    private List<String> listOfFiles;
    private boolean isOpen;
    private DatagramSocket serverSocket;
    /**
     * Create the server with the port and address of the PI. Besides, a list is created
     */
    public Server() {
        // the port on which the server is listening on:
        port = Protocol.PI_PORT;
        // a new list is created that stores all files that are available on the server:
        listOfFiles = new ArrayList<>();
        // after creating the server, it is not opened yet:
        isOpen = false;
    }

    /**
     * Start the server. Can only be done when server is not open for connections yet. A new DatagramSocket should start
     * with port of PI as input. This socket can be connected to the client to only accept packets that are sent by that
     * client. The boolean isOpen should be set on true as the server is now open for connections.
     */
    public void start() {
        if (isOpenForConnection()) {
            System.out.println("Server is already in use.");
            return;
        } else {
            try {
                serverSocket = new DatagramSocket(port);
                ClientHandler clientHandler = new ClientHandler(serverSocket, this);
                clientHandler.start();
                System.out.println("clientHandler started");
            } catch (SocketException e) {
                System.out.println("Not able to start the server with this port.");
            }
        }
        isOpen = true;
    }

    /**
     * Stop the server. Can only be done if server is opened for connections before. The boolean isOpen should be set on
     * false as the server is now closed for connections.
     */
    public void stop() {
        if (!isOpenForConnection()) {
            System.out.println("Server is not even opened yet.");
            return;
        }
        serverSocket.close();
        isOpen = false;
        System.out.println("Server is closed");
    }

    /**
     * Check whether the server is currently open for accepting connections.
     *
     * @return true if the server is open, false if not.
     */
    public boolean isOpenForConnection() {
        return isOpen;
    }

    /**
     * Receive a file from the client on the server (PI).
     *
     * @param fileName is the file to be received.
     */
    public void receiveFile(String fileName) {
        // todo
    }

    /**
     * Send a file to the client from the server (PI).
     *
     * @param fileName is the file to be sent.
     */
    public void sendFile(String fileName) {
        // todo
    }

    /**
     * Remove a file from the server (PI).
     *
     * @param fileName is the file to be removed.
     */
    public void removeFile(String fileName) {
        // todo
    }

    /**
     * Replace a file on the server (PI).
     *
     * @param oldFileName is the file to be replaced.
     * @param newFileName is the new file to be uploaded.
     */
    public void replaceFile(String oldFileName, String newFileName) {
        removeFile(oldFileName);
        receiveFile(newFileName);
    }

    /**
     * List the files that are located on the server (PI).
     */
    public void listFiles() {
        // todo
    }

    /**
     * Respond to an unknown request by the client.
     */
    public void respondToUnknownRequest() {
        // todo
    }
}

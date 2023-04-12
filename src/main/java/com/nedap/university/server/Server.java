package com.nedap.university.server;

import com.nedap.university.PacketProtocol;

import java.io.IOException;
import java.net.*;
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
    private String fileName;
    private String oldFileName;
    private String newFileName;

    /**
     * Create the server with the port and address of the PI. Besides, a list is created
     */
    public Server() {
        // the port on which the server is listening on:
        port = PacketProtocol.PI_PORT;
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
     * Set fileName to the name of the file that is requested by the client.
     * @param fileName is the name of the file.
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Set oldFileName to the name of the file that is requested to be replaced by the client.
     * @param oldFileName is the name of the old (= to be replaced) file.
     */
    public void setOldFileName(String oldFileName) {
        this.oldFileName = oldFileName;
    }

    /**
     * Set newFileName to the name of the file that is requested by the client.
     * @param newFileName is the name of the new file.
     */
    public void setNewFileName(String newFileName) {
        this.newFileName = newFileName;
    }

    public void respondToClientRequest(int flag, InetAddress inetAddress, int port, DatagramSocket serverSocket) {
        String responseMessage = null;
        int sequenceNumber = PacketProtocol.generateRandomSequenceNumber();
        if (flag == PacketProtocol.UPLOAD) {
            responseMessage = ("Server successfully received the request for uploading " + fileName);
        } else if (flag == PacketProtocol.DOWNLOAD) {
            responseMessage = ("Server successfully received the request for downloading " + fileName);
        } else if (flag == PacketProtocol.REMOVE) {
            responseMessage = ("Server successfully received the request for removing " + fileName);
        } else if (flag == PacketProtocol.REPLACE) {
            responseMessage = ("Server successfully received the request for replacing " + oldFileName + " by " + newFileName);
        } else if (flag == PacketProtocol.LIST) {
            responseMessage = ("Server successfully received the request for listing all available files.");
        } else if (flag == PacketProtocol.CLOSE) {
            responseMessage = ("Server successfully received the request for closing the connection.");
        }
        byte[] response = PacketProtocol.createPacketWithHeader(sequenceNumber, 0, PacketProtocol.ACK, responseMessage.getBytes());
        DatagramPacket responsePacket = new DatagramPacket(response, response.length, inetAddress, port);
        try {
            serverSocket.send(responsePacket);
        } catch (IOException e) {
            throw new RuntimeException(e); //todo
        }
    }

    /**
     * Receive a file from the client on the server (PI).
     *
     * @param fileName is the file to be received.
     */
    public void receiveFile(String fileName, InetAddress inetAddress, int port, DatagramSocket serverSocket) {
        respondToClientRequest(PacketProtocol.UPLOAD, inetAddress, port, serverSocket);
        // todo
    }

    /**
     * Send a file to the client from the server (PI).
     *
     * @param fileName is the file to be sent.
     */
    public void sendFile(String fileName, InetAddress inetAddress, int port, DatagramSocket serverSocket) {
        respondToClientRequest(PacketProtocol.DOWNLOAD, inetAddress, port, serverSocket);
        // todo
    }

    /**
     * Remove a file from the server (PI).
     *
     * @param fileName is the file to be removed.
     */
    public void removeFile(String fileName, InetAddress inetAddress, int port, DatagramSocket serverSocket) {
        respondToClientRequest(PacketProtocol.REMOVE, inetAddress, port, serverSocket);

        // todo
    }

    /**
     * Replace a file on the server (PI).
     *
     * @param oldFileName is the file to be replaced.
     * @param newFileName is the new file to be uploaded.
     */
    public void replaceFile(String oldFileName, String newFileName, InetAddress inetAddress, int port, DatagramSocket serverSocket) {
        respondToClientRequest(PacketProtocol.REPLACE, inetAddress, port, serverSocket);
//        removeFile(oldFileName, inetAddress, port, serverSocket);
//        receiveFile(newFileName, inetAddress, port, serverSocket);
        // todo
    }

    /**
     * List the files that are located on the server (PI).
     */
    public void listFiles(InetAddress inetAddress, int port, DatagramSocket serverSocket) {
        respondToClientRequest(PacketProtocol.LIST, inetAddress, port, serverSocket);
        // todo
    }

    /**
     * Close the connection between server and client as response to the close request by the client.
     */
    public void closeConnection(InetAddress inetAddress, int port, DatagramSocket serverSocket) {
        respondToClientRequest(PacketProtocol.CLOSE, inetAddress, port, serverSocket);
        // todo
    }

}
